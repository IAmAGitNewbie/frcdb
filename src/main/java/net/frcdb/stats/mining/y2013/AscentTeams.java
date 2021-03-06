package net.frcdb.stats.mining.y2013;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.frcdb.api.game.event.Game;
import net.frcdb.api.game.standing.AscentStanding;
import net.frcdb.api.game.team.TeamEntry;
import net.frcdb.api.team.Team;
import net.frcdb.db.Database;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author tim
 */
public class AscentTeams {
	private Game game;
	private String url;

	private Logger logger = LoggerFactory.getLogger(AscentStanding.class);
	
	public AscentTeams(Game game) {
		this.game = game;
		
		url = game.getStandingsURL();
	}

	public List<TeamEntry> parse() throws IOException {
		List<TeamEntry> ret = new ArrayList<TeamEntry>();

		Database db = Database.getInstance();
		
		Document doc;
		try {
			doc = Jsoup.parse(new URL(url), 5000);
		} catch (Exception ex) {
			logger.warn("Failed fetching standings at " + url, ex);
			return ret;
		}
		
		Elements rows = doc.select("div.Section1 table");
		
		Iterator<Element> tableIter = rows.iterator();
		tableIter.next(); // skip 2, selector won't work for some reason
		tableIter.next();
		
		Element table = tableIter.next();
		
		for (Element row : table.select("tr:gt(1)")) { // skip first 2 headers
			// 0: rank
			// 1: team #
			// 2: qualification score
			// 3: hybrid points
			// 4: bridge points
			// 5: teleop points
			// 6: coopertition points
			// 7: record (wins-losses-ties)
			// 8: disqualifications
			// 9: matches played
			
			Team team = db.getTeam(Integer.parseInt(row.child(1).text()));
			if (team == null) {
				logger.warn("Unknown team, skipping: " + row.child(0).text());
				continue;
			}
			
			if (game.getEntry(team) != null) {
				// skip if already exists
				continue;
			}
			
			TeamEntry entry = game.createEntry(team);
			ret.add(entry);
		}

		return ret;
	}
	
}
