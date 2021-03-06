package net.frcdb.export;

import com.googlecode.objectify.Ref;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import net.frcdb.api.event.Event;
import net.frcdb.api.game.event.Game;
import net.frcdb.api.game.event.GameType;
import net.frcdb.api.game.event.element.GameOPRProvider;
import net.frcdb.api.game.match.Match;
import net.frcdb.api.game.match.MatchType;
import net.frcdb.api.game.standing.Standing;
import net.frcdb.api.game.team.TeamEntry;
import net.frcdb.api.game.team.element.*;
import net.frcdb.api.team.Team;
import net.frcdb.db.Database;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author tim
 */
public class GamesImport {
	
	public static final String GAMES_FILE = "export/games.json";
	
	private Logger logger = LoggerFactory.getLogger(GamesImport.class);
	private Database db;
	
	public GamesImport(InputStream input) throws IOException {
		db = new Database();
		
		logger.info("Parsing game data...");
		
		long time = System.currentTimeMillis();
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(input);

		double secs = (System.currentTimeMillis() - time) / 1000d;
		logger.info("Finished in " + secs + " seconds; now importing games...");
		
		// parse single node if needed
		if (rootNode.isArray()) {
			Iterator<JsonNode> iter = rootNode.getElements();
			while (iter.hasNext()) {
				JsonNode gameNode = iter.next();
				parse(gameNode);
			}
		} else {
			logger.info("Parsing single node...");
			parse(rootNode);
		}
	}
	
	private void parse(JsonNode gameNode) {
		int year = gameNode.get("gameYear").asInt();
		String eventName = gameNode.get("eventShortName").asText();
		
		Event event = db.getEventByShortName(eventName);
		
		logger.info("Creating game for " + event + " (" + eventName + ")");
		
		Game g = GameType.getGame(year).create(event);
		
		// parse general info
		g.setStartDate(new Date(gameNode.get("startDate").asLong()));
		g.setEndDate(new Date(gameNode.get("endDate").asLong()));
		
		if (gameNode.has("eid")) {
			g.setEid(gameNode.get("eid").asInt());
		}
		
		Database.save().entity(g).now();
		
		// parse teams
		JsonNode teamsNode = gameNode.get("teams");
		Iterator<JsonNode> teamIterator = teamsNode.getElements();
		while (teamIterator.hasNext()) {
			JsonNode teamNode = teamIterator.next();
			parseTeamEntry(teamNode, g);
		}
		
		Database.save().entity(g).now();
		
		JsonNode matchesNode = gameNode.get("matches");
		Iterator<JsonNode> matchIterator = matchesNode.getElements();
		while (matchIterator.hasNext()) {
			JsonNode matchNode = matchIterator.next();
			parseMatch(matchNode, g);
		}
		
		Database.save().entity(g).now();
		
		JsonNode standingsNode = gameNode.get("standings");
		Iterator<JsonNode> standingIterator = standingsNode.getElements();
		while (standingIterator.hasNext()) {
			JsonNode standingNode = standingIterator.next();
			
			try {
				parseStanding(standingNode, g);
			} catch (Exception ex) {
				logger.error("Failed to parse standing, referenced team may"
						+ " not exist? " + standingNode.toString(), ex);
			}
		}
		
		Database.save().entity(g).now();
		
		// parse opr/dpr last
		if (g instanceof GameOPRProvider) {
			GameOPRProvider go = (GameOPRProvider) g;
			if (gameNode.has("opr")) {
				JsonNode opr = gameNode.get("opr");
				
				go.setAverageOPR(opr.get("average").asDouble());
				go.setTotalOPR(opr.get("total").asDouble());
				
				if (opr.has("highest")) {
					go.setHighestOPR(opr.get("highest").asDouble());
					int highestTeam = opr.get("highestTeam").asInt();
					go.setHighestOPRTeam(g.getEntry(db.getTeam(highestTeam)));
				}
				
				if (opr.has("lowest")) {
					go.setLowestOPR(opr.get("lowest").asDouble());
					int lowestTeam = opr.get("lowestTeam").asInt();
					go.setLowestOPRTeam(g.getEntry(db.getTeam(lowestTeam)));
				}
			}
			
			if (gameNode.has("dpr")) {
				JsonNode dpr = gameNode.get("dpr");
				
				go.setAverageDPR(dpr.get("average").asDouble());
				go.setTotalDPR(dpr.get("total").asDouble());
			}
		}
		
		// append to the event
		event.getGameReferences().add(Ref.create(g));
		
		Database.save().entities(g, event).now();
		
		logger.info("Finished importing: " + g);
	}
	
	private void parseTeamEntry(JsonNode node, Game g) {
		int teamNumber = node.get("number").asInt();
		Team team = db.getTeam(teamNumber);
		
		if (team == null) {
			logger.warn("Team " + teamNumber + " not found! Skipping...");
			return;
		}
		
		TeamEntry te = g.createEntry(team);
		te.setRank(node.get("rank").asInt());
		te.setMatchesPlayed(node.get("matchesPlayed").asInt());
		te.setWins(node.get("wins").asInt());
		te.setLosses(node.get("losses").asInt());
		te.setTies(node.get("ties").asInt());
		
		if (node.has("finalMatchLevel")) {
			String typeString = node.get("finalMatchLevel").asText();
			te.setFinalMatchLevel(MatchType.getMatchType(typeString));
		}
		
		// other properties
		
		if (te instanceof Seeding2007Provider) {
			Seeding2007Provider s = (Seeding2007Provider) te;
			
			if (node.has("qualificationScore")) {
				s.setQualificationScore(
						(float) node.get("qualificationScore").asDouble());
				s.setRankingScore((float) node.get("rankingScore").asDouble());
			}
		}
		
		if (te instanceof MatchPointsProvider) {
			MatchPointsProvider p = (MatchPointsProvider) te;
			
			if (node.has("matchPoints")) {
				p.setMatchPoints(node.get("matchPoints").asInt());
			}
		}
		
		if (te instanceof Seeding2010Provider) {
			Seeding2010Provider p = (Seeding2010Provider) te;
			
			if (node.has("seedingScore")) {
				p.setSeedingScore((float) node.get("seedingScore").asDouble());
				p.setCoopertitionBonus(
						(float) node.get("coopertitionBonus").asDouble());
			}
		}
		
		if (te instanceof HangingProvider) {
			HangingProvider p = (HangingProvider) te;
			
			if (node.has("hangingPoints")) {
				p.setHangingPoints((float) node.get("hangingPoints").asDouble());
			}
		}
		
		if (te instanceof OPRProvider) {
			OPRProvider p = (OPRProvider) te;
			
			if (node.has("opr")) {
				p.setOPR(node.get("opr").asDouble());
				p.setDPR(node.get("dpr").asDouble());
			}
		}
		
		Database.save().entity(te).now();
		g.getTeamReferences().add(Ref.create(te));
	}
	
	private void parseMatch(JsonNode node, Game g) {
		Match m = new Match(g);
		
		m.setType(MatchType.getMatchType(node.get("type").asText()));
		m.setNumber(node.get("number").asInt());
		m.setTime(node.get("time").asText());
		
		m.setRedScore(node.get("redScore").asInt());
		Iterator<JsonNode> redTeams = node.get("redTeams").getElements();
		while (redTeams.hasNext()) {
			JsonNode t = redTeams.next();
			Team team = db.getTeam(t.asInt());
			if (team == null) {
				logger.warn("Team not found in match #" + m.getNumber() + ": "
						+ t.asText());
				continue;
			}
			
			m.addRedTeam(team);
		}
		
		m.setBlueScore(node.get("blueScore").asInt());
		Iterator<JsonNode> blueTeams = node.get("blueTeams").getElements();
		while (blueTeams.hasNext()) {
			JsonNode t = blueTeams.next();
			Team team = db.getTeam(t.asInt());
			if (team == null) {
				logger.warn("Team not found in match #" + m.getNumber() + ": "
						+ t.asText());
				continue;
			}
			
			m.addBlueTeam(team);
		}
		
		Database.save().entity(m).now();
		
		switch (m.getType()) {
			case QUALIFICATION:
				g.getQualificationMatchReferences().add(Ref.create(m));
				break;
			case QUARTERFINAL:
				g.getQuarterfinalsMatchReferences().add(Ref.create(m));
				break;
			case SEMIFINAL:
				g.getSemifinalsMatchReferences().add(Ref.create(m));
				break;
			case FINAL:
				g.getFinalsMatchReferences().add(Ref.create(m));
		}
	}

	private void parseStanding(JsonNode node, Game g) {
		Standing s = g.createStanding();
		
		s.setRank(node.get("rank").asInt());
		
		Team team = db.getTeam(node.get("team").asInt());
		if (team == null) {
			logger.warn("Team not found: " + node.get("team").asText());
			return;
		}
		
		s.setTeam(g.getEntry(team));
		s.setMatchesPlayed(node.get("matchesPlayed").asInt());
		
		if (s instanceof Seeding2007Provider) {
			Seeding2007Provider p = (Seeding2007Provider) s;
			
			if (node.has("qualificationScore")) {
				p.setQualificationScore((float) 
						node.get("qualificationScore").asDouble());
			}
			
			if (node.has("rankingScore")) {
				p.setRankingScore((float) node.get("rankingScore").asDouble());
			}
		}
		
		if (s instanceof MatchPointsProvider) {
			MatchPointsProvider p = (MatchPointsProvider) s;
			
			if (node.has("matchPoints")) {
				p.setMatchPoints(node.get("matchPoints").asInt());
			}
		}
	
		if (s instanceof Seeding2010Provider) {
			Seeding2010Provider p = (Seeding2010Provider) s;
			
			if (node.has("seedingScore")) {
				p.setSeedingScore((float) node.get("seedingScore").asDouble());
			}
			
			if (node.has("coopertitionBonus")) {
				p.setCoopertitionBonus(
						(float) node.get("coopertitionBonus").asDouble());
			}
		}
		
		if (s instanceof HangingProvider) {
			HangingProvider p = (HangingProvider) s;
			
			if (node.has("hangingPoints")) {
				p.setHangingPoints((float) node.get("hangingPoints").asDouble());
			}
		}
		
		Database.save().entity(s).now();
		g.getStandingReferences().add(Ref.create(s));
	}
	
	public static void main(String[] args) throws IOException {
		File file = new File(GAMES_FILE);
		if (!file.exists()) {
			System.err.println("[Error] Not found: export/games.json");
			return;
		}
		
		GamesImport em = new GamesImport(file.toURI().toURL().openStream());
	}
	
}
