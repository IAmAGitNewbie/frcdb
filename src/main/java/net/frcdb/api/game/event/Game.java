package net.frcdb.api.game.event;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import net.frcdb.api.event.Event;
import net.frcdb.api.game.event.element.GameOPRProvider;
import net.frcdb.api.game.match.Match;
import net.frcdb.api.game.match.MatchType;
import net.frcdb.api.game.standing.Standing;
import net.frcdb.api.game.team.TeamEntry;
import net.frcdb.api.game.team.element.*;
import net.frcdb.api.team.Team;
import net.frcdb.db.Database;
import net.frcdb.stats.calc.Statistic;
import net.frcdb.stats.mining.EventMining;
import net.frcdb.util.Sources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: finish updating to use no linked classes. Consider weak caching? (Keep
 * old linked getters, and have them fetch on demand from database. They can
 * cache the value in a transient field for quick access later?)
 * @author tim
 */
@Cache
@Entity
public abstract class Game {

	public static final Class PROP_TEAM_OPR = OPRProvider.class;
	public static final Class PROP_TEAM_HANGING = HangingProvider.class;
	public static final Class PROP_TEAM_MATCH_POINTS = MatchPointsProvider.class;
	public static final Class PROP_TEAM_SEEDING_2007 = Seeding2007Provider.class;
	public static final Class PROP_TEAM_SEEDING_2010 = Seeding2010Provider.class;
	
	public static final Class PROP_GAME_OPR = GameOPRProvider.class;

	@Ignore
	private transient Logger logger = LoggerFactory.getLogger(Game.class);
	
	@Parent
	@Load
	private Ref<Event> event;
	
	@Id
	private long gameYear;
	
	/** for queries by game year - can't use the id */
	@Index
	private int gameYearIndex;
	
	private int eid;
	
	private String date;
	
	@Index
	private Date startDate;
	
	@Index
	private Date endDate;
	
	private String resultsURL;
	private String standingsURL;
	private String awardsURL;
	
	@Index private List<Ref<Match>> qualificationMatches;
	@Index private List<Ref<Match>> quarterfinalsMatches;
	@Index private List<Ref<Match>> semifinalsMatches;
	@Index private List<Ref<Match>> finalsMatches;
	@Index private List<Ref<TeamEntry>> teams;
	@Index private List<Ref<Standing>> standings;
	
	@Load private Ref<Game> parent;
	@Load private List<Ref<Game>> children;

	public Game() {
		gameYear = getGameYear();
		gameYearIndex = getGameYear();
		
		qualificationMatches = new ArrayList<Ref<Match>>();
		quarterfinalsMatches = new ArrayList<Ref<Match>>();
		semifinalsMatches = new ArrayList<Ref<Match>>();
		finalsMatches = new ArrayList<Ref<Match>>();
		
		teams = new ArrayList<Ref<TeamEntry>>();
		standings = new ArrayList<Ref<Standing>>();
		children = new ArrayList<Ref<Game>>();
	}
	
	public Game(Event event) {
		this.event = Ref.create(event);
		
		gameYear = getGameYear();
		gameYearIndex = getGameYear();
		
		qualificationMatches = new ArrayList<Ref<Match>>();
		quarterfinalsMatches = new ArrayList<Ref<Match>>();
		semifinalsMatches = new ArrayList<Ref<Match>>();
		finalsMatches = new ArrayList<Ref<Match>>();
		
		teams = new ArrayList<Ref<TeamEntry>>();
		standings = new ArrayList<Ref<Standing>>();
		children = new ArrayList<Ref<Game>>();
	}

	/**
	 * Sets the game year indexed value. This should pretty much never be
	 * called.
	 * @param gameYearIndex it's a mystery!
	 */
	public void setGameYearIndex(int gameYearIndex) {
		this.gameYearIndex = gameYearIndex;
	}
	
	public void setEvent(Event event) {
		this.event = Ref.create(event);
	}
	
	public Event getEvent() {
		return event.get();
	}
	
	/**
	 * Gets the FIRST-provided eid (ID_Event) that can be used to check equality
	 * to their data.
	 * @return 
	 */
	public int getEid() {
		return eid;
	}

	public void setEid(int eid) {
		this.eid = eid;
	}

	/**
	 * The year the game was made, e.g. for Breakaway, "2010"
	 * @return the game year
	 */
	public abstract int getGameYear();
	
	/**
	 * The name of the game, e.g. for 2010, "Breakaway"
	 * @return the game name
	 */
	public abstract String getGameName();

	/**
	 * Gets or creates an EventMining implementation to gather statistics.
	 * @return an EventMining instance
	 */
	public abstract EventMining getMiner();

	/**
	 * Creates a new game-specific TeamEntry.
	 * @return a new TeamEntry
	 */
	public abstract TeamEntry createEntry(Team team);
	
	/**
	 * Creates a new Standing specific to this Game implementation.
	 * @return an empty standing appropriate for this Game
	 */
	public abstract Standing createStanding();

	/**
	 * Returns a list of properties support by this Game implementation.
	 * @return a list of implemented interfaces
	 */
	public abstract Class[] getGameProperties();
	
	/**
	 * Returns a list of supported properties for TeamEntries of this Game.
	 * These should be provided as classes of the different interfaces
	 * implemented by TeamEntry subclasses.
	 * @return a list of implemented interfaces
	 */
	public abstract Class[] getTeamProperties();
	
	/**
	 * Returns a list of supported properties for Standing entries of this Game.
	 * These should be provided as classes of the different interfaces
	 * implemented by the Standing classes associated with this Game.
	 * @return a list of implemented interfaces
	 */
	public abstract Class[] getStandingProperties();

	/**
	 * Returns a list of statistics that should be run for the given event.
	 * @return an array containing the statistics to be run
	 */
	public abstract Statistic[] getStatistics();

	public boolean hasGameProperty(Class clazz) {
		for (Class c : getGameProperties()) {
			if (c.equals(clazz)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks that the given team property is supported by the given event.
	 * @param clazz The class of the property to check
	 * @return true if supported/implemented, false otherwise
	 */
	public boolean hasTeamProperty(Class clazz) {
		for (Class c : getTeamProperties()) {
			if (c.equals(clazz)) {
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Checks that the given standing property is supported by the given event.
	 * @param clazz The class of the property to check
	 * @return true if supported/implemented, false otherwise
	 */
	public boolean hasStandingProperty(Class clazz) {
		for (Class c : getStandingProperties()) {
			if (c.equals(clazz)) {
				return true;
			}
		}

		return false;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
	public List<Ref<Match>> getQualificationMatchReferences() {
		return qualificationMatches;
	}
	
	public Collection<Match> getQualificationMatches() {
		return Database.ofy().load().refs(qualificationMatches).values();
	}
	
	public List<Ref<Match>> getQuarterfinalsMatchReferences() {
		return quarterfinalsMatches;
	}
	
	public Collection<Match> getQuarterfinalsMatches() {
		return Database.ofy().load().refs(quarterfinalsMatches).values();
	}

	public List<Ref<Match>> getSemifinalsMatchReferences() {
		return semifinalsMatches;
	}
	
	public Collection<Match> getSemifinalsMatches() {
		return Database.ofy().load().refs(semifinalsMatches).values();
	}
	
	public List<Ref<Match>> getFinalsMatchReferences() {
		return finalsMatches;
	}
	
	public Collection<Match> getFinalsMatches() {
		return Database.ofy().load().refs(finalsMatches).values();
	}
	
	public List<Ref<Match>> getAllMatchReferences() {
		List<Ref<Match>> ret = new ArrayList<Ref<Match>>(
				qualificationMatches.size()
				+ quarterfinalsMatches.size()
				+ semifinalsMatches.size()
				+ finalsMatches.size());
		ret.addAll(qualificationMatches);
		ret.addAll(quarterfinalsMatches);
		ret.addAll(semifinalsMatches);
		ret.addAll(finalsMatches);
		return ret;
	}
	
	public Collection<Match> getAllMatches() {
		return Database.ofy().load().refs(getAllMatchReferences()).values();
	}
	
	public List<Match> getMatches(MatchType type) {
		return Database.getInstance().getMatches(this, type);
	}

	public List<Match> getMatches(Team t) {
		return Database.getInstance().getMatches(this, t);
	}

	public Match getMatch(MatchType type, int number) {
		return Database.getInstance().getMatch(this, type, number);
	}

	public List<Ref<TeamEntry>> getTeamReferences() {
		return teams;
	}
	
	public Collection<TeamEntry> getTeams() {
		return Database.ofy().load().refs(teams).values();
	}

	public TeamEntry getEntry(Team t) {
		return Database.getInstance().getEntry(this, t);
	}

	public String getFullEventName() {
		return event.get().getName();
	}

	public String getResultsURL() {
		return resultsURL;
	}

	public void setResultsURL(String resultsURL) {
		this.resultsURL = resultsURL;
	}

	/**
	 * Updates the match results. The caller is responsible for storing this
	 * event after the update has completed.
	 * Note that events that have not occurred will return a 404 error. This is
	 * silently ignored.
	 * @param db The database to get team data from
	 * @throws IOException On I/O error
	 */
	public void updateResults() throws IOException {
		if (resultsURL == null) {
			logger.warn("No match results URL defined, using default: " 
					+ this);
			resultsURL = Sources.getResultsURL(this);
		}

		Database.getInstance().purgeMatches(this);

		List<Match> matches = getMiner().getMatches(this);
		
		for (Match m : matches) {
			Database.save().entity(m).now();
			
			switch (m.getType()) {
				case QUALIFICATION:
					qualificationMatches.add(Ref.create(m));
					break;
				case QUARTERFINAL:
					quarterfinalsMatches.add(Ref.create(m));
					break;
				case SEMIFINAL:
					semifinalsMatches.add(Ref.create(m));
					break;
				case FINAL:
					finalsMatches.add(Ref.create(m));
			}
		}
	}
	
	public String getStandingsURL() {
		return standingsURL;
	}

	public void setStandingsURL(String standingsURL) {
		this.standingsURL = standingsURL;
	}

	/**
	 * Updates the event standings. The caller is responsible for storing this
	 * game after the update has completed.
	 * Note that events that have not occurred will return a 404 error. This is
	 * silently ignored.
	 * @param db The database to get team data from
	 * @throws IOException On I/O error
	 */
	public void updateStandings() throws IOException {
		if (standingsURL == null) {
			logger.warn("No standings URL defined for " + this 
					+ ", using defaults");
			standingsURL = Sources.getStandingsURL(this);
		}
		
		List<Standing> nstandings = getMiner().getStandings(this);
		if (nstandings == null || nstandings.isEmpty()) {
			logger.warn("No standings data available: " + standingsURL);
			return;
		}
		
		logger.info(this + " now has " + nstandings.size()
				+ " standings.");

		Database.getInstance().purgeStandings(this);

		for (Standing s : nstandings) {
			Database.save().entity(s).now();
			standings.add(Ref.create(s));
			
			TeamEntry te = s.getTeam();

			if (te == null) {
				logger.warn("Team referenced by standing is missing");
				continue;
			}

			te.setRank(s.getRank());
			te.setMatchesPlayed(s.getMatchesPlayed());

			standingParseExtra(te, s);

			Database.save().entity(te);
		}
	}
	
	public Collection<Standing> getStandings() {
		return Database.ofy().load().refs(standings).values();
	}

	public List<Ref<Standing>> getStandingReferences() {
		return standings;
	}
	
	/**
	 * Subclasses can utilize this to handle other year-specific properties.
	 * @param standing The standing to handle, can be casted
	 */
	public void standingParseExtra(TeamEntry entry, Standing standing) {
		
	}
	
	/**
	 * Updates teams based on standings data. The new teams will be persisted
	 * to the datastore automatically, but this Game will need to be saved in
	 * order to persist team references properly. Note that an event must have
	 * at least started in order to call this. Preliminary team lists must be
	 * fetched with the EID for this game when the game is first crawled. This
	 * is non-destructive and will only add new (previously missing) teams.
	 * @throws IOException On I/O error
	 */
	public void updateTeams() throws IOException {
		if (standingsURL == null) {
			logger.warn("No standings URL defined for " + this 
					+ ", using defaults");
			standingsURL = Sources.getStandingsURL(this);
		}
		
		List<TeamEntry> newTeams = getMiner().getTeams(this);
		if (newTeams == null || newTeams.isEmpty()) {
			logger.warn("No team data available, or no teams missing: "
					+ standingsURL);
			return;
		}

		for (TeamEntry t : newTeams) {
			Database.getInstance().store(t);
			teams.add(Ref.create(t));
		}
	}
	

	public String getAwardsURL() {
		return awardsURL;
	}

	public void setAwardsURL(String awardsURL) {
		this.awardsURL = awardsURL;
	}

	public Game getParent() {
		if (parent == null) {
			return null;
		}
		
		return Database.ofy().load().ref(parent).get();
	}

	public Ref<Game> getParentReference() {
		return parent;
	}
	
	public void setParent(Game parent) {
		if (parent == null) {
			this.parent = null;
			return;
		}
		
		this.parent = Ref.create(parent);
	}
	
	public List<Ref<Game>> getChildReferences() {
		return children;
	}

	public Collection<Game> getChildren() {
		return Database.ofy().load().refs(children).values();
	}
	
	public void addChild(Game g) {
		children.add(Ref.create(g));
	}
	
	public void removeChild(Game g) {
		children.remove(Ref.create(g));
	}
	
	public boolean containsChild(Game g) {
		Ref<Game> ref = Ref.create(g);
		
		for (Ref<Game> r : children) {
			if (r.equivalent(ref)) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return "Game["
				+ "event=" + event.get().getShortName() + ", "
				+ "year=" + getGameYear() + ", "
				+ "name=" + getGameName()
				+ "]";
	}

}
