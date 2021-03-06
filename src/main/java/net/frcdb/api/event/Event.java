package net.frcdb.api.event;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.frcdb.api.game.event.Game;
import net.frcdb.db.Database;

/**
 *
 * @author tim
 */
@Cache
@Entity
public class Event {

	/**
	 * The year to show default data from.
	 */
	public static final int CURRENT_YEAR = 2013;
	
	@Parent
	private Key<EventRoot> parent;

	/**
	 * The full event name, including sponsors, etc
	 */
	@Index
	private String name;

	/**
	 * A short, identifiable, and plain-English event name.
	 */
	@Index
	@Id
	private String shortName;

	/**
	 * The event ID, usually the state abbreviation or similar.
	 */
	@Index
	private String identifier;

	private String venue;
	private String city;
	private String state;
	private String country;
	
	private double latitude;
	private double longitude;
	
	/**
	 * A list of other possible names for this event
	 */
	private List<String> aliases;
	
	private List<Ref<Game>> games;
	
	@Index
	private int hits;

	public Event() {
		parent = Key.create(EventRoot.get());
		
		games = new ArrayList<Ref<Game>>();
	}
	
	public Event(String shortName) {
		parent = Key.create(EventRoot.get());
		
		games = new ArrayList<Ref<Game>>();
		
		this.shortName = shortName;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	public String getVenue() {
		return venue;
	}

	public void setVenue(String venue) {
		this.venue = venue;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public Collection<Game> getGames() {
		return Database.ofy().load().refs(games).values();
	}
	
	public List<Ref<Game>> getGameReferences() {
		return games;
	}

	public Game getGame(int year) {
		return Database.getInstance().getGame(this, year);
	}

	/**
	 * Gets the latest game for this event.
	 * @return the current game
	 */
	public Game getGame() {
		return Database.getInstance().getLatestGame(this);
	}
	
	public List<String> getAliases() {
		return aliases;
	}
	
	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}
	
	public void addAlias(String alias) {
		if (aliases == null) {
			aliases = new ArrayList<String>();
		}
		
		aliases.add(alias);
	}
	
	public void removeAlias(String alias) {
		if (aliases == null) {
			aliases = new ArrayList<String>();
			return;
		}
		
		aliases.remove(alias);
	}
	
	public boolean isChampionship() {
		return name.equals("FIRST Championship");
	}

	public int getHits() {
		return hits;
	}

	public void setHits(int hits) {
		this.hits = hits;
	}

	@Override
	public String toString() {
		return "Event["
			+ "shortName=" + shortName + ", "
			+ "identifier=" + identifier
			+ "]";
	}
	
}
