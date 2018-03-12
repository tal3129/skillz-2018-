package bots.goodOlBot;

import bots.goodOlBot.Actions.Sails;
import bots.goodOlBot.Data.BotData;
import pirates.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NOT APPROVED!  ~%~  Official seal of Approval by Ido Asraff as a DISGUSTING CLASS.  ~%~
 */

public class MapMethods {
    private static final int ASTRO_CAREFUL_DISTANCE = 20;
    public static int boardX = 6400; // board x
    public static int boardY = 6400;
    public static PirateGame game;


    // pirate speed: 200
    //push distance: 600
    //push range: 300


    //returns the closest mapObject between 2 objects
    public static <T extends MapObject, E extends MapObject> T closer(E cmpObj, T mo1, T mo2) {
        if (cmpObj.distance(mo1) < cmpObj.distance(mo2))
            return mo1;
        else
            return mo2;
    }

    //returns the closest mapObject between 2 objects
    public static <T extends MapObject, E extends MapObject> T further(E cmpObj, T mo1, T mo2) {
        if (cmpObj.distance(mo1) > cmpObj.distance(mo2))
            return mo1;
        else
            return mo2;
    }

    //returns the closest mapObject from an array
    public static <T extends MapObject, E extends MapObject> T closest(E cmpObj, T[] objArr) {
        if (objArr.length == 0 || cmpObj == null)
            return null;
        T closest = objArr[0];
        for (T obj : objArr)
            closest = closer(cmpObj, closest, obj);
        return closest;
    }

    //returns the closest mapObject from a List
    public static <T extends MapObject, E extends MapObject> T closest(E cmpObj, List<T> objArr) {
        if (objArr.size() == 0 || cmpObj == null)
            return null;
        T closest = objArr.get(0);
        for (T obj : objArr)
            closest = closer(cmpObj, closest, obj);
        return closest;
    }


    //recives a cmpObject, an array and a size, returns a list od the closest objects to the cmp
    public static <T extends MapObject, E extends MapObject> List<T> closestToObjectList(E cmpObj, T[] objArray, int arraySize) {
        List<T> closestList = new ArrayList<>();
        List<T> allObjects = new ArrayList<>(Arrays.asList(objArray));
        while (closestList.size() < arraySize && !allObjects.isEmpty()) {
            closestList.add(closest(cmpObj, allObjects));
            allObjects.remove(closest(cmpObj, allObjects));
        }
        return closestList;
    }

    //recives a cmpObject, an array and a size, returns a list od the closest objects to the cmp
    public static <T extends MapObject, E extends MapObject> List<T> closestToObjectList(E cmpObj, List<T> objList, int arraySize) {
        List<T> closestList = new ArrayList<>();
        List<T> allObjects = new ArrayList<>(objList);
        while (closestList.size() < arraySize && !allObjects.isEmpty()) {
            closestList.add(closest(cmpObj, allObjects));
            allObjects.remove(closest(cmpObj, allObjects));
        }
        return closestList;
    }

    //recives a cmpObject, an array and a size, returns a list version sorted by distance
    public static <T extends MapObject, E extends MapObject> List<T> toSortedByDistanceList(E cmpObj, T[] objArray) {
        List<T> closestList = new ArrayList<>();
        List<T> allObjects = new ArrayList<>(Arrays.asList(objArray));
        while (closestList.size() < objArray.length && !allObjects.isEmpty()) {
            closestList.add(closest(cmpObj, allObjects));
            allObjects.remove(closest(cmpObj, allObjects));
        }
        return closestList;
    }

    //recives a cmpObject, an array and a size, returns a list version sorted by distance
    public static <T extends MapObject, E extends MapObject> List<T> toSortedByDistanceList(E cmpObj, List<T> objArray) {

        List<T> closestList = new ArrayList<>();
        List<T> allObjects = new ArrayList<>(objArray);

        while (closestList.size() < objArray.size() && !allObjects.isEmpty()) {
            closestList.add(closest(cmpObj, allObjects));
            allObjects.remove(closest(cmpObj, allObjects));
        }
        return closestList;
    }

    public static boolean isCloserThan(MapObject dest, MapObject cmpObj, MapObject[] objects) {
        return closer(dest, cmpObj, closest(dest, objects)).equals(cmpObj);
    }

    //the closest capsule which is not carried by a pirate
    public static Capsule closestAvailableCapsules(MapObject cmpObj) {
        Capsule closestCapsule = null;
        for (Capsule capsule : game.getMyCapsules())
            if (capsule.holder == null)
                if (closestCapsule == null)
                    closestCapsule = capsule;
                else
                    closestCapsule = closer(cmpObj, closestCapsule, capsule);
        return closestCapsule;
    }

    //if we can push enemy to his te immediate death with fullPushDistance return the direction we should push towards
    public static Location getDeathLocation(Pirate enemy, int fullPushDistance) {
        Location dl = pushOutLocal(enemy, fullPushDistance);
        if (dl == null)
            dl = onAstroLocation(enemy, fullPushDistance);
        return dl;
    }


    // if we can push an enemy to an astroid next location return the direction
    public static Location onAstroLocation(Pirate enemy, int fullPushDistance) {
        for (Asteroid asteroid : game.getLivingAsteroids()) {
            if (asteroid.location.add(asteroid.direction).distance(enemy.location.towards(asteroid, fullPushDistance)) < asteroid.size - 200)
                return enemy.location.towards(asteroid, fullPushDistance);
        }

        return null;
    }

    //if we can push enemy out of bounds return the direction we should push towards to do so
    public static Location pushOutLocal(Pirate enemy, int fullPushDistance) {
        int x = enemy.location.col;
        int y = enemy.location.row;
        if (x - fullPushDistance < -enemy.maxSpeed)
            return new Location(y, -enemy.maxSpeed);
        if (x + fullPushDistance > boardX + enemy.maxSpeed)
            return new Location(y, boardX + enemy.maxSpeed);
        if (y - fullPushDistance < -enemy.maxSpeed)
            return new Location(-enemy.maxSpeed, x);
        if (y + fullPushDistance > boardY + enemy.maxSpeed)
            return new Location(boardY + enemy.maxSpeed, x);

        return null; //return null if  cant
    }

    //if we can push enemy out of bounds return the direction we should push towards to do so
    public static Location pushOutLocal(Location enemyLoc, int speed, int fullPushDistance) {
        int x = enemyLoc.col;
        int y = enemyLoc.row;
        if (x - fullPushDistance < -speed)
            return new Location(y, -speed);
        if (x + fullPushDistance > boardX + speed)
            return new Location(y, boardX + speed);
        if (y - fullPushDistance < -speed)
            return new Location(-speed, x);
        if (y + fullPushDistance > boardY + speed)
            return new Location(boardY + speed, x);

        return null; //return null if  cant
    }


    //returns the location to sail towards to get to the destination in minimum time
    public static Location getShorterRoute(MapObject dest, Pirate pirate) {
        MapObject betterDest = dest;
        boolean destIsHole = false;
        int minTurns = turnsToObject(pirate, dest);
        for (Wormhole wormhole : game.getAllWormholes()) {
            int disWithHole = pirate.distance(wormhole.location.towards(pirate, wormhole.size)) + wormhole.partner.distance(dest);
            int turnsWithHole = ((disWithHole + pirate.maxSpeed - 1) / pirate.maxSpeed) + wormhole.turnsToReactivate;
            //if it will take me less time to go through the hole than going normally
            if (turnsWithHole < minTurns) {
                destIsHole = true;
                minTurns = turnsWithHole;
                betterDest = wormhole.location.towards(pirate, game.wormholeRange);
            }
        }
        if (destIsHole)
            betterDest = getShorterRoute(betterDest, pirate);
        return betterDest.getLocation();
    }

    private static int upToZero(int x) {
        return x < 0 ? 0 : x;
    }


    public static int turnsToObject(Pirate pirate, MapObject dest) {
        return (pirate.distance(dest) + pirate.maxSpeed - 1) / pirate.maxSpeed;
    }


    public static void setGame(PirateGame game) {
        MapMethods.game = game;
    }

    public static Location locInNTurns(MapObject ass, Location direction, int n) {
        Location loc = ass.getLocation();
        for (int i = 0; i < n; i++)
            loc = loc.add(direction);
        return loc;
    }

    public static Location astroDodge(Pirate p, Location dest) { //returns next location we need to go to to get away from asteroids, if it's fine as it is return the pirate's destination

        boolean needToDoge = false; //doge
        Asteroid hitter = null;

        for (Asteroid ass : game.getLivingAsteroids()) {
            for (int i = 1; i < 10; i++) { //if staying in place kills me
                if (!needToDoge && (locInNTurns(ass, ass.direction, i).distance(p.getLocation()) < ass.size + ASTRO_CAREFUL_DISTANCE)) {
                    hitter = ass;
                    needToDoge = true;
                }
            }
        }

        if (needToDoge) { //check if we can we run away, if so do this
            Location loc1 = new Location(hitter.direction.col, -hitter.direction.row);
            Location loc2 = new Location(-hitter.direction.col, hitter.direction.row);
            return further(hitter, p.location.add(loc1), p.location.add(loc2));
        }
        return dest;
    }

    public static boolean willBeHitByAstro(MapObject loc) {
        return Arrays.stream(game.getLivingAsteroids()).anyMatch(asteroid -> asteroid.location.add(asteroid.direction).distance(loc) < asteroid.size + 50);
    }

    public static void initializeStatics(PirateGame game) {
        boardX = game.cols;
        boardY = game.rows;
    }

    public static List<Capsule> getAvalibleCapsules() {
        List<Capsule> res = new ArrayList<>();
        for (Capsule capsule : game.getMyCapsules()) {
            if (capsule.holder == null) //if it's a mine
                res.add(capsule);
        }
        return res;
    }

    //if all enemies are going towards me (maybe consider if enemies go to my next location) can i go straight to the dest without meeting any enemies that can shoot me

    public static boolean canGoStraightCareFully(MapObject leader, Pirate[] enemies, MapObject destObj, int enemyForTurns) {
        Location leaderLocation = leader.getLocation();
        Location dest = destObj.getLocation();
        //get enemies original positions
        List<Location> enemyLocations = Arrays.stream(enemies).map(pirate -> pirate.location).collect(Collectors.toList());
        int runs = 0;
        boolean result = true;

        //give enemies enemyForTurns turns ahead
        for (int i = 0; i < enemyForTurns; i++) {
            for (Pirate enemy : enemies) {
                enemy.location = enemy.location.towards(destObj.getLocation().towards(leader, game.mothershipUnloadRange), game.pirateMaxSpeed);
                enemy.pushReloadTurns--;
            }

            runs++;
        }

        //while we still cant unload
        while (leaderLocation.distance(dest) > game.mothershipUnloadRange) {
            if (Sails.directPiratesThreatsOnLoc(leaderLocation, enemies) > 1) { //if someone caught me ): return false - we cant reach the unload stuff . WAS >ZERO BUT CHANGED TO >1 CUZ NOBODY PUSHES WITH ONE PIRATE
                result = false;
                break;
            }

            for (Pirate enemy : enemies) {
                enemy.location = enemy.location.towards(leaderLocation, game.pirateMaxSpeed);
                enemy.pushReloadTurns--;
            }

            runs++;
            leaderLocation = leaderLocation.towards(dest, game.pirateMaxSpeed);

        }

        // returns the enemies to their former state
        for (int i = 0, enemiesLength = enemies.length; i < enemiesLength; i++) {
            Pirate enemy = enemies[i];
            enemy.location = enemyLocations.get(i);
            enemy.pushReloadTurns += runs;
        }

        return result;
    }

    public static boolean canGoStraightCareFullyByPirate(Pirate leader, Pirate[] enemies, MapObject destObj, int enemyForTurns) {
        Location leaderLocation = leader.getLocation();
        Location dest = destObj.getLocation();
        //get enemies original positions
        List<Location> enemyLocations = Arrays.stream(enemies).map(pirate -> pirate.location).collect(Collectors.toList());
        int runs = 0;
        boolean result = true;

        //give enemies enemyForTurns turns ahead
        for (int i = 0; i < enemyForTurns; i++) {
            for (Pirate enemy : enemies) {
                enemy.location = enemy.location.towards(destObj.getLocation().towards(leader, game.mothershipUnloadRange), game.pirateMaxSpeed);
                enemy.pushReloadTurns--;
            }

            runs++;
        }

        //while we still cant unload
        while (leaderLocation.distance(dest) > game.mothershipUnloadRange) {
            if (Sails.directPiratesThreatsOnLoc(leaderLocation, enemies) > leader.numPushesForCapsuleLoss - 1) { //if someone caught me ): return false - we cant reach the unload stuff . WAS >ZERO BUT CHANGED TO >1 CUZ NOBODY PUSHES WITH ONE PIRATE
                result = false;
                break;
            }

            for (Pirate enemy : enemies) {
                enemy.location = enemy.location.towards(leaderLocation, game.pirateMaxSpeed);
                enemy.pushReloadTurns--;
            }

            runs++;
            leaderLocation = leaderLocation.towards(dest, leader.maxSpeed);

        }

        // returns the enemies to their former state
        for (int i = 0, enemiesLength = enemies.length; i < enemiesLength; i++) {
            Pirate enemy = enemies[i];
            enemy.location = enemyLocations.get(i);
            enemy.pushReloadTurns += runs;
        }

        return result;
    }

    //if all enemies are going towards my destination (maybe consider if enemies go to my next location) can i go straight to the dest without meeting any enemies that can shoot me
    public static boolean canGoStraightAgainstSmartDefenseCareFully(MapObject leader, Pirate[] enemies, MapObject destObj, int enemyForTurns) {
        Location leaderLocation = leader.getLocation();
        Location dest = destObj.getLocation();
        //get enemies original positions
        List<Location> enemyLocations = Arrays.stream(enemies).map(pirate -> pirate.location).collect(Collectors.toList());
        int runs = 0;
        boolean result = true;

        //give enemies enemyForTurns turns ahead
        for (int i = 0; i < enemyForTurns; i++) {
            for (Pirate enemy : enemies) {
                enemy.location = enemy.location.towards(destObj.getLocation().towards(leader, game.mothershipUnloadRange), game.pirateMaxSpeed);
                enemy.pushReloadTurns--;
            }
            runs++;
        }

        //while we still cant unload
        while (leaderLocation.distance(dest) > game.mothershipUnloadRange) {
            if (Sails.directPiratesThreatsOnLoc(leaderLocation, enemies) > 1) { //if someone caught me ): return false - we cant reach the unload stuff . WAS >ZERO BUT CHANGED TO >1 CUZ NOBODY PUSHES WITH ONE PIRATE
                result = false;
                break;
            }

            for (Pirate enemy : enemies) {
                enemy.location = enemy.location.towards(destObj.getLocation().towards(leader, game.mothershipUnloadRange), game.pirateMaxSpeed);
                enemy.pushReloadTurns--;
            }

            runs++;
            leaderLocation = leaderLocation.towards(dest, game.pirateMaxSpeed);

        }

        // returns the enemies to their former state
        for (int i = 0, enemiesLength = enemies.length; i < enemiesLength; i++) {
            Pirate enemy = enemies[i];
            enemy.location = enemyLocations.get(i);
            enemy.pushReloadTurns += runs;
        }

        return result;
    }

    //assuming the defense doesnt know how to switch
    public static boolean canGoStraightAgainstStupidDefenseCareFully(MapObject leader, Pirate[] enemies, MapObject destObj, int enemyForTurns) {
        Location leaderLocation = leader.getLocation();
        Location dest = destObj.getLocation();
        //get enemies original positions
        List<Location> enemyLocations = Arrays.stream(enemies).map(pirate -> pirate.location).collect(Collectors.toList());
        int runs = 0;
        boolean result = true;

        //give enemies enemyForTurns turns ahead
        for (int i = 0; i < enemyForTurns; i++) {
            for (Pirate enemy : enemies) {
                enemy.location = enemy.location.towards(destObj.getLocation().towards(leader, game.mothershipUnloadRange), game.pirateMaxSpeed);
                enemy.pushReloadTurns--;
            }

            runs++;
        }

        //while we still cant unload
        while (leaderLocation.distance(dest) > game.mothershipUnloadRange) {
            if (Sails.directPiratesThreatsOnLoc(leaderLocation, enemies) > 1) { //if someone caught me ): return false - we cant reach the unload stuff . WAS >ZERO BUT CHANGED TO >1 CUZ NOBODY PUSHES WITH ONE PIRATE
                result = false;
                break;
            }

            for (Pirate enemy : enemies) {
                enemy.location = enemy.location.towards(destObj, enemy.maxSpeed);
                enemy.pushReloadTurns--;
            }

            runs++;
            leaderLocation = leaderLocation.towards(dest, game.pirateMaxSpeed);
        }

        // returns the enemies to their former state
        for (int i = 0, enemiesLength = enemies.length; i < enemiesLength; i++) {
            Pirate enemy = enemies[i];
            enemy.location = enemyLocations.get(i);
            enemy.pushReloadTurns += runs;
        }

        return result;
    }

    public static Pirate getClosestEnemyPirateWithCapsule() {
        List<Pirate> capsuleCarriers = new ArrayList<>();

        for (Pirate pirate : game.getEnemyLivingPirates()) {
            if (pirate.hasCapsule())
                capsuleCarriers.add(pirate);
        }
        if (capsuleCarriers.isEmpty())
            return null;
        capsuleCarriers = toSortedByDistanceList(game.getEnemyMotherships()[0], capsuleCarriers);
        return capsuleCarriers.get(0);
    }

    public static List<Pirate> getCarryBoys(Capsule[] capsules) {
        List<Pirate> res = new ArrayList<>();
        for (Capsule capsule : capsules)
            if (capsule.holder != null)
                res.add(capsule.holder);
        return res;
    }

    /**
     * @param a      the row of the circle center
     * @param b      the col of the circle center
     * @param radius the enemyRadius of the circle
     * @return all the locations inside an on the circle and in map
     */
    public static List<Location> allLocationInCircle(int a, int b, int radius) {
        List<Location> res = new ArrayList<>();
        int deg;
        int jumpDeg = 4;
        int jumpRad = 4;
        while (radius >= 0) {
            deg = 0;
            while (deg < 360) {
                Location loc = new Location(a + (int) (Math.round(Math.cos(deg * radius))), b + (int) (Math.round(Math.sin(deg * radius))));
                if (loc.inMap())
                    res.add(loc);
                deg += jumpDeg;
            }
            radius -= jumpRad;
        }
        return res;
    }

    public static List<Pirate> getAllEnemyCapsulesCarriers() {
        return Arrays.stream(game.getEnemyLivingPirates()).filter(pirate -> pirate.capsule != null).collect(Collectors.toList());
    }


    public static Location getAvarageLoc(MapObject[] objArr) {
        Location loc = new Location(0, 0);

        for (MapObject obj : objArr)
            loc = loc.add(obj.getLocation());

        return new Location(loc.row / objArr.length, loc.col / objArr.length);
    }

    public static int calculateExplosionValue(Location expLoc, int expRange) {

        int normalPirateValue = 1;
        int heavyValue = 1;
        int capsValue = 2;

        int value = 0;
        for (Pirate pirate : game.getEnemyLivingPirates()) {
            if (pirate.distance(expLoc) < expRange) {
                value += normalPirateValue;
                if (pirate.stateName.equals("heavy"))
                    value += heavyValue;
                if (pirate.capsule != null)
                    value += capsValue;
            }
        }
        for (Pirate pirate : game.getMyLivingPirates()) {
            if (pirate.distance(expLoc) < expRange) {
                value -= normalPirateValue;
                if (pirate.stateName.equals("heavy"))
                    value -= heavyValue;
                if (pirate.capsule != null)
                    value -= capsValue;
            }
        }
        return value;
    }

    public static Location bestPlaceForStickyBombToGoOff(Location start, int pushDistance, int expRange)//
    {
        int bestSoFar = Integer.MIN_VALUE;
        Location bestLoc = new Location(0, 0);

        for (int deg = 0; deg < 360; deg++) {
            Location l = new Location((int) Math.round(Math.cos(deg) * pushDistance), (int) Math.round((Math.sin(deg) * pushDistance)));
            l = l.add(start);

            int currentScore = calculateExplosionValue(l, expRange);
            if (currentScore > bestSoFar) {
                bestSoFar = currentScore;
                bestLoc = l;
            }
        }

        return bestLoc;
    }

    public static int distanceFromLine(Location object,Location p1,Location p2)
    {
        int a = p2.col-p1.col;
        int b = p1.row-p2.row;
        int c = p1.col*(p2.row-p1.row)+p1.row*(p1.col-p2.col);
        return Math.abs(a*object.row+b*object.col+c)/(int)Math.round(Math.sqrt(a*a+b*b));

    }





    // returns a location to stray towards in order to stick on an enemy or to help a friendly
    // 1. connect the 2 arrays of living pirates into a list
    // 2. filter only those which are carry boys, their next location will be in my range if I will go to it.
    // 3. if they are friendly, make sure I will be able to push them and that they are offside. if not, make sure I will have a sticky to stick on them
    // 4. map them to their next location if they continue to their base
    // 5. return the closest of these location
    public static Location strayTowardsCapsule(Pirate pirate) {
        return closest(pirate, Stream.concat(Arrays.stream(game.getMyLivingPirates()), Arrays.stream(game.getEnemyLivingPirates())).collect(Collectors.toList()).stream().filter(carryBoi -> carryBoi.hasCapsule() && carryBoi.owner == game.getMyself() ? pirate.pushReloadTurns <= 1 && BotData.getInstance().getUnit(carryBoi).offside : game.getMyself().turnsToStickyBomb <= 1 && carryBoi.location.towards(MapMethods.closest(carryBoi, carryBoi.owner == game.getMyself() ? game.getMyMotherships() : game.getEnemyMotherships()), carryBoi.maxSpeed).distance(pirate) < pirate.maxSpeed + pirate.pushRange - 100 && carryBoi.stickyBombs.length == 0).map(carryBoi -> carryBoi.location.towards(MapMethods.closest(carryBoi, carryBoi.owner == game.getMyself() ? game.getMyMotherships() : game.getEnemyMotherships()), carryBoi.maxSpeed)).collect(Collectors.toList()));
    }

    /**
     *
     * todo
     * move away from friendlies when we have sticky
     *maybe upgrade attack tictac
     *MyBotCompetitive 230 attacking pirates don't move problem
     *
     * create offside by pushing an enemy pirate
     * careful sail works for pushes too, so we push our friend in an angle safely to offside
     * shoot asteroids to the enemy's next next loc if they can't push
     * push each other if you are going into a bad spot where you may lose your capsule
     * do not try attack that was proved not helpful system
     * not get into portal
     * IMPORTANT check if we really need canGoStraghtAgainstSmartCarefuly against everyone
     * */

}