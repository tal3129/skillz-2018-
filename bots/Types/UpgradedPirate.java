package bots.Types;

import bots.Data.BotData;
import bots.MapMethods;
import pirates.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UpgradedPirate {

    public Pirate pirate;
    private Location lastLocation;
    private Location direction; //location is used as vector2 here
    private Location nextLocation;
    PirateGame game;

    public int pushedCount; // the amount of times the enemy was pushed

    private int certainty;
    private int predictionId;
    int a;

    public boolean acted;

    // todobomb or without wormholes
    private static final int WAIT_TO_DETERMINE_RADIUS = 10;
    public static final int DIVIDE_DISTANCES = 20;
    public static final int ASSUMED_MAX_RADIUS = 2000;
    public static final int UNIT_DISTANCE = ASSUMED_MAX_RADIUS / DIVIDE_DISTANCES;
    public List<Integer> assumedRadiuses = new ArrayList<>(Collections.nCopies(DIVIDE_DISTANCES, 0));

    public UpgradedPirate(Pirate mObject) {
        a = 1;
        this.pirate = mObject;
        this.lastLocation = pirate.location;
        this.direction = new Location(0, 0);
    }

    public int getCertainty() {
        return certainty;
    }

    public int getPredictionId() {
        return predictionId;
    }

    public void setCertainty(int certainty) {
        this.certainty = certainty;
    }

    public void setPredictionId(int predictionId) {
        this.predictionId = predictionId;
    }


    //called once a turn and updates direction, speed, (and enemyRadius maybe)
    public void updateDirectionAndLoc() {
        this.game = BotData.getInstance().game;

        // if he stays in the same distance to my mothersip, he is probably a defender
        Mothership closeMothership = MapMethods.closest(pirate, game.getMyMotherships());

        if (closeMothership != null && pirate.location.distance(closeMothership) < ASSUMED_MAX_RADIUS && game.getEnemy().equals(pirate.owner)) {
            int i = pirate.location.distance(closeMothership) / UNIT_DISTANCE;
            assumedRadiuses.set(i, assumedRadiuses.get(i) + 1);
        }

        direction = pirate.getLocation().subtract(lastLocation == null ? pirate.getLocation() : lastLocation);
        lastLocation = pirate.getLocation();

        nextLocation = pirate.location.add(direction);
        // get the closest hole that will be active next turn
        Wormhole closeHole = MapMethods.closest(nextLocation, Arrays.stream(game.getAllWormholes()).filter(wormhole -> wormhole.turnsToReactivate <= 1).collect(Collectors.toList()));
        if (closeHole!=null && nextLocation.distance(closeHole) <= closeHole.wormholeRange)
            nextLocation = closeHole.partner.location;

        pushedCount = 0;
        acted = false;
    }


    public Location getNextLocation() {
        return nextLocation;
    }

    public void setNextLocation(Location nextLocation) {
        this.nextLocation = nextLocation;
    }

    public Pirate getPirate() {
        return pirate;
    }

    public void setPirate(Pirate pirate) {
        this.pirate = pirate;
    }

    //return the object's location after n turns, if he continues in his current direction
    public Location afterNTurns(int n) {
        return UpgradedPirate.afterNTurns(lastLocation, direction, n);
    }

    //assumes direction is constant
    //I'm aware that it can be more efficient...
    public Location[] futureLocations(int size) {
        return UpgradedPirate.futureLocations(lastLocation, direction, size);
    }

    public static Location afterNTurns(Location lastLocation, Location direction, int n) {
        Location res = lastLocation;
        for (int i = 0; i < n; i++)
            res = res.add(direction);
        return res;
    }

    //returns an array of the object's next locations if it continues
    public static Location[] futureLocations(Location lastLocation, Location direction, int size) {
        return IntStream.range(0, size).mapToObj(i -> UpgradedPirate.afterNTurns(lastLocation, direction, i)).toArray(Location[]::new);
    }

    public void setLastLocation(Location location) {
        this.lastLocation = location;
    }

    public Location getLastLocation() {
        return lastLocation;
    }
}