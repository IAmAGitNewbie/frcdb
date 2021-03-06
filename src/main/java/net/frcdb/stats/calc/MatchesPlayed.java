package net.frcdb.stats.calc;

import java.util.HashMap;
import java.util.Map;
import net.frcdb.api.game.event.Game;
import net.frcdb.api.game.match.Match;
import net.frcdb.api.game.team.TeamEntry;
import net.frcdb.api.team.Team;
import net.frcdb.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Counts qualification matches.
 * @author tim
 */
public class MatchesPlayed implements GameStatistic {

	private Logger logger = LoggerFactory.getLogger(MatchesPlayed.class);
	
	@Override
	public String[] getNames() {
		return new String[] {"matchesplayed", "matches", "played"};
	}

	@Override
	public void calculate(Game game) {
		Map<Team, Integer> count = new HashMap<Team, Integer>();
		
		for (Match m : game.getQualificationMatches()) {
			for (Team t : m.getTeams()) {
				if (count.get(t) == null) {
					count.put(t, 1);
				} else {
					count.put(t, count.get(t) + 1);
				}
			}
		}
		
		for (Team t : count.keySet()) {
			TeamEntry entry = game.getEntry(t);
			if (entry == null) {
				logger.warn("Null team entry for " + t);
				continue;
			}
			
			entry.setMatchesPlayed(count.get(t));
			Database.save().entity(entry);
		}
	}

	@Override
	public String getBackendName() {
		return null;
	}

}
