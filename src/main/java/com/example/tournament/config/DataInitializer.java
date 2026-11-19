package com.example.tournament.config;

import com.example.tournament.dto.request.CreateTeamRequest;
import com.example.tournament.entity.*;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.StandingRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.PlayerRepository;
import com.example.tournament.repository.UserAccountRepository;
import com.example.tournament.service.TeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final StandingRepository standingRepository;
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TeamService teamService;

    public DataInitializer(TournamentRepository tournamentRepository,
                           TeamRepository teamRepository,
                           PlayerRepository playerRepository,
                           MatchRepository matchRepository,
                           StandingRepository standingRepository,
                           UserAccountRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           TeamService teamService) {
        this.tournamentRepository = tournamentRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.standingRepository = standingRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.teamService = teamService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            seedUsers();
        }
        if (tournamentRepository.count() == 0 && teamRepository.count() == 0) {
            seedData();
        }
        // Compétitions européennes : création idempotente (ajoute la CL + les 5 ligues
        // si elles ne sont pas encore présentes, sans écraser l'existant).
        seedEuropeanCompetitions();
    }

    private void seedUsers() {
        userRepository.save(newUser("admin", "admin123", Role.ADMIN));
        userRepository.save(newUser("manager", "manager123", Role.MANAGER));
        userRepository.save(newUser("operator", "operator123", Role.OPERATOR));
        userRepository.save(newUser("user", "user123", Role.USER));
    }

    private UserAccount newUser(String username, String rawPassword, Role role) {
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setRole(role);
        account.setEnabled(true);
        return account;
    }

    private void seedData() {
        // Équipes de démonstration (Ligue 1)
        String[][] teams = {
            {"Paris SG", "Paris", "Luis Enrique"},
            {"Marseille", "Marseille", "Roberto De Zerbi"},
            {"Lyon", "Lyon", "Pierre Sage"},
            {"Monaco", "Monaco", "Adi Hutter"},
            {"Lille", "Lille", "Bruno Genesio"},
            {"Nice", "Nice", "Francesco Farioli"},
            {"Rennes", "Rennes", "Julien Stephan"},
            {"Nantes", "Nantes", "Antoine Kombouare"}
        };

        Team[] teamEntities = new Team[teams.length];
        for (int i = 0; i < teams.length; i++) {
            Team team = new Team();
            team.setName(teams[i][0]);
            team.setCity(teams[i][1]);
            team.setCoach(teams[i][2]);
            team.setCountry(Country.FRANCE);
            team.setStatus(TeamStatus.ACTIVE);
            team = teamRepository.save(team);
            teamEntities[i] = team;

            // 2 joueurs par équipe
            for (int j = 1; j <= 2; j++) {
                Player player = new Player();
                player.setFirstName("Joueur");
                player.setLastName(teams[i][0] + " " + j);
                player.setPosition(j == 1 ? "Attaquant" : "Gardien");
                player.setNumber(j);
                team.addPlayer(player);
                playerRepository.save(player);
            }
        }

        // Championnat aller-retour avec 6 équipes
        Tournament championship = new Tournament();
        championship.setName("Championnat de France");
        championship.setSport("Football");
        championship.setDescription("Saison de championnat démonstrative");
        championship.setCountry(Country.FRANCE);
        championship.setStartDate(LocalDate.now().plusDays(7));
        championship.setEndDate(LocalDate.now().plusMonths(3));
        championship.setLocation("Paris");
        championship.setMaxTeams(6);
        championship.setType(TournamentType.CHAMPIONNAT_ALLER_RETOUR);
        championship.setStatus(TournamentStatus.BROUILLON);

        for (int i = 0; i < 6; i++) {
            championship.addTeam(teamEntities[i]);
        }
        championship.setStatus(TournamentStatus.INSCRIPTION);
        tournamentRepository.save(championship);

        // Tournoi à élimination directe avec 8 équipes
        Tournament knockout = new Tournament();
        knockout.setName("Coupe Nationale");
        knockout.setSport("Football");
        knockout.setDescription("Coupe à élimination directe");
        knockout.setCountry(Country.FRANCE);
        knockout.setStartDate(LocalDate.now().plusDays(30));
        knockout.setEndDate(LocalDate.now().plusMonths(2));
        knockout.setLocation("Lyon");
        knockout.setMaxTeams(8);
        knockout.setType(TournamentType.ELIMINATION_DIRECTE);
        knockout.setStatus(TournamentStatus.BROUILLON);

        for (Team team : teamEntities) {
            knockout.addTeam(team);
        }
        knockout.setStatus(TournamentStatus.INSCRIPTION);
        tournamentRepository.save(knockout);
    }

    /**
     * Crée la Ligue des Champions (internationale, inscription manuelle) et les
     * championnats des 5 grands pays européens. Les clubs de chaque pays sont créés
     * s'ils n'existent pas puis inscrits automatiquement dans la ligue de leur pays.
     * Idempotent : ne touche à rien si les compétitions existent déjà.
     */
    private void seedEuropeanCompetitions() {
        // Rattrapage : les compétitions démo créées avant l'ajout du champ Pays.
        backfillCountry("Championnat de France", Country.FRANCE);
        backfillCountry("Coupe Nationale", Country.FRANCE);

        // Ligue des Champions : tournoi international, inscription manuelle par
        // l'administrateur / l'opérateur (pas de pays => pas d'auto-inscription).
        if (!tournamentRepository.existsByNameIgnoreCase("Ligue des Champions")) {
            Tournament champions = new Tournament();
            champions.setName("Ligue des Champions");
            champions.setSport("Football");
            champions.setDescription("Compétition européenne inter-clubs — inscription manuelle des meilleurs clubs");
            champions.setStartDate(LocalDate.now().plusDays(14));
            champions.setEndDate(LocalDate.now().plusMonths(9));
            champions.setLocation("Europe");
            champions.setMaxTeams(16);
            champions.setType(TournamentType.POULES_ELIMINATION);
            champions.setStatus(TournamentStatus.INSCRIPTION);
            tournamentRepository.save(champions);
            logger.info("Ligue des Champions créée (poules → élimination, 16 places)");
        } else {
            // Rattrapage : la LdC était en élimination directe avant le format poules.
            tournamentRepository.findByNameIgnoreCase("Ligue des Champions").ifPresent(t -> {
                if (t.getType() == TournamentType.ELIMINATION_DIRECTE
                        && matchRepository.findByTournamentId(t.getId()).isEmpty()) {
                    t.setType(TournamentType.POULES_ELIMINATION);
                    tournamentRepository.save(t);
                    logger.info("Ligue des Champions convertie en format poules → élimination");
                }
            });
        }

        seedLeague("Premier League", Country.ENGLAND, "Championnat d'Angleterre de football", 20,
                new String[][] {
                    {"Manchester City", "Manchester", "Pep Guardiola"},
                    {"Arsenal", "Londres", "Mikel Arteta"},
                    {"Liverpool", "Liverpool", "Arne Slot"},
                    {"Aston Villa", "Birmingham", "Unai Emery"},
                    {"Tottenham", "Londres", "Ange Postecoglou"},
                    {"Chelsea", "Londres", "Enzo Maresca"},
                    {"Manchester United", "Manchester", "Ruben Amorim"},
                    {"Newcastle", "Newcastle", "Eddie Howe"}
                });

        seedLeague("La Liga", Country.SPAIN, "Championnat d'Espagne de football", 20,
                new String[][] {
                    {"Real Madrid", "Madrid", "Carlo Ancelotti"},
                    {"FC Barcelone", "Barcelone", "Hansi Flick"},
                    {"Atlético Madrid", "Madrid", "Diego Simeone"},
                    {"Athletic Bilbao", "Bilbao", "Ernesto Valverde"},
                    {"Real Sociedad", "Saint-Sébastien", "Imanol Alguacil"},
                    {"Girona", "Girona", "Míchel"},
                    {"Séville", "Séville", "García Pimienta"},
                    {"Valence", "Valence", "Rubén Baraja"}
                });

        seedLeague("Serie A", Country.ITALY, "Championnat d'Italie de football", 20,
                new String[][] {
                    {"Inter", "Milan", "Simone Inzaghi"},
                    {"AC Milan", "Milan", "Paulo Fonseca"},
                    {"Juventus", "Turin", "Thiago Motta"},
                    {"Atalanta", "Bergame", "Gian Piero Gasperini"},
                    {"Bologne", "Bologne", "Vincenzo Italiano"},
                    {"AS Rome", "Rome", "Claudio Ranieri"},
                    {"Lazio", "Rome", "Baroni"},
                    {"Napoli", "Naples", "Antonio Conte"}
                });

        seedLeague("Bundesliga", Country.GERMANY, "Championnat d'Allemagne de football", 18,
                new String[][] {
                    {"Bayer Leverkusen", "Leverkusen", "Xabi Alonso"},
                    {"VfB Stuttgart", "Stuttgart", "Sebastian Hoeneß"},
                    {"Bayern Munich", "Munich", "Vincent Kompany"},
                    {"RB Leipzig", "Leipzig", "Marco Rose"},
                    {"Borussia Dortmund", "Dortmund", "Nuri Sahin"},
                    {"Eintracht Francfort", "Francfort", "Dino Toppmöller"},
                    {"Werder Brême", "Brême", "Ole Werner"},
                    {"VfL Wolfsburg", "Wolfsburg", "Ralph Hasenhüttl"}
                });

        seedLeague("Ligue 1", Country.FRANCE, "Championnat de France de football", 18,
                new String[][] {
                    {"Paris SG", "Paris", "Luis Enrique"},
                    {"Marseille", "Marseille", "Roberto De Zerbi"},
                    {"Lyon", "Lyon", "Pierre Sage"},
                    {"Monaco", "Monaco", "Adi Hutter"},
                    {"Lille", "Lille", "Bruno Genesio"},
                    {"Nice", "Nice", "Francesco Farioli"},
                    {"Rennes", "Rennes", "Julien Stephan"},
                    {"Nantes", "Nantes", "Antoine Kombouare"}
                });
    }

    private void backfillCountry(String tournamentName, Country country) {
        tournamentRepository.findByNameIgnoreCase(tournamentName)
                .filter(t -> t.getCountry() == null)
                .ifPresent(t -> {
                    t.setCountry(country);
                    tournamentRepository.save(t);
                });
    }

    private void seedLeague(String name, Country country, String description, int maxTeams,
                            String[][] clubs) {
        if (tournamentRepository.existsByNameIgnoreCase(name)) {
            return;
        }

        Tournament league = new Tournament();
        league.setName(name);
        league.setSport("Football");
        league.setDescription(description);
        league.setCountry(country);
        league.setStartDate(LocalDate.now().plusDays(7));
        league.setEndDate(LocalDate.now().plusMonths(8));
        league.setLocation(country.getLabel());
        league.setMaxTeams(maxTeams);
        league.setType(TournamentType.CHAMPIONNAT_ALLER_RETOUR);
        league.setStatus(TournamentStatus.BROUILLON);
        league = tournamentRepository.save(league);

        for (String[] club : clubs) {
            Team team = ensureTeam(club[0], club[1], club[2], country);
            // Inscription manuelle sans passer par le service : dans ce seed
            // transactionnel, une exception du proxy marquerait toute la transaction
            // rollback-only. Le club peut déjà être inscrit par l'auto-inscription.
            Tournament current = tournamentRepository.findById(league.getId()).orElse(league);
            if (!current.isTeamRegistered(team) && !current.isFull()) {
                current.addTeam(team);
                if (!standingRepository.existsByTournamentIdAndTeamId(current.getId(), team.getId())) {
                    standingRepository.save(new Standing(current, team));
                }
                tournamentRepository.save(current);
            }
        }

        Tournament reloaded = tournamentRepository.findById(league.getId()).orElse(league);
        if (reloaded.getStatus() == TournamentStatus.BROUILLON) {
            reloaded.setStatus(TournamentStatus.INSCRIPTION);
            tournamentRepository.save(reloaded);
        }
        logger.info("Ligue créée : {} ({}, {} équipes)", name, country.getLabel(), clubs.length);
    }

    /**
     * Retourne le club existant (en lui rattachant son pays s'il en manque un),
     * ou le crée via TeamService — ce qui déclenche l'inscription automatique
     * dans les ligues ouvertes de son pays.
     */
    private Team ensureTeam(String name, String city, String coach, Country country) {
        Team existing = teamRepository.findByNameIgnoreCase(name).orElse(null);
        if (existing != null) {
            if (existing.getCountry() == null) {
                existing.setCountry(country);
                return teamRepository.save(existing);
            }
            return existing;
        }

        CreateTeamRequest request = new CreateTeamRequest();
        request.setName(name);
        request.setCity(city);
        request.setCoach(coach);
        request.setCountry(country);
        teamService.createTeam(request);
        return teamRepository.findByNameIgnoreCase(name).orElseThrow();
    }
}
