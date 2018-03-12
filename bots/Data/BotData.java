package bots.Data;

import bots.*;
import bots.Actions.Sails;
import bots.Predictions.PredictionManager;
import bots.Types.Unit;
import bots.Types.UpgradedPirate;
import pirates.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * holds the bot's data - his pirate squads, asteroid and enemy info, etc.
 * <p>
 * APPROVED!  ~%~  Official seal of Approval by Ido Asraff as a DISGUSTING CLASS.  ~%~
 */

public class BotData {

    //finals
    public static final int MIN_DISTANCE_FROM_MS_FOR_STICKING = 1400; // the minimum distance from their base to stick a sticky bomb
    public static final int ENEMY_DEFENDERS_FOR_TURNS = 1; //how many turns we give for the enemies "ahead" when detecting offside
    public static final int STRAY_TURNS = 10; // the maximum amount of turns attacking pirates will stray to catch an enemy capsule
    public static final int STRAYER_AMOUNT = 2;
    public static final int MIN_VALUE_FOR_SUPER_PUSH = 1;
    public static int CAREFUL_RELOAD_TURNS = 5; // if you are in careful sail, don't get into range of enemies that will be able to shoot in these turns
    //end finals

    private static BotData instance;
    public static PirateGame game;

    public final int DEVIATION_LOCATIONS = 25; //max distance between locations still means we've predicted well

    public int CAREFUL_DISTANCE = 0; //careful distance to get around enemies and asteroids
    public int MAX_BOMB_COUNTDOWN_SAFE = 2; // don't get into range of bombs that will explode in these turns

    public Location[] myMines; //list of my mines
    public Location[] enemyMines; //list of enemy mines
    public List<Integer> asteroidsPushedIds; //ids of asteroids we already pushed this turn
    public List<UpgradedPirate> upgradedEnemies; //all enemy pirates, upgraded
    public List<UpgradedPirate> upgradedPirates; // all friendly pirathes, upgraded

    public List<Pirate> piratesWithSticky; //all friendly pirates with a sticky bomb on them
    public List<Unit> units; //include 2 pirates, one of them has capsule, unit members follow the leader
    public List<Pirate> attackingPirates; //the attacking team with the carrier and its helpers, might be divided

    public int enemyRadius = 0;
    public int enemySecondRadius = 0;

    public Asteroid astroOnOurBase = null; // the asteroid that is resting on our base


    public PredictionManager predictionManager; //include all our predictions. is used to make predictions (sets nextLocation of upgradedEnemies) and to evaluate reliability


    public static void initialize(PirateGame game) {
        instance = new BotData(game);
    }

    // the constructor of botData initializes the arrays and lists that needs to be initialized ONCE
    private BotData(PirateGame game) {
        this.game = game;

        // the array that counts pushes made in a turn, so a pirate won't push out a pirate that would already be pushed out
        attackingPirates = new ArrayList<>();

        myMines = new Location[game.getMyCapsules().length];
        for (int i = 0; i < myMines.length; i++)
            myMines[i] = game.getMyCapsules()[i].location;

        enemyMines = new Location[game.getEnemyCapsules().length];
        for (int i = 0; i < enemyMines.length; i++)
            enemyMines[i] = game.getEnemyCapsules()[i].location;


        upgradedPirates = new ArrayList<>();
        for (Pirate p : game.getAllMyPirates())
            upgradedPirates.add(new UpgradedPirate(p));

        upgradedEnemies = new ArrayList<>();
        for (Pirate p : game.getAllEnemyPirates())
            upgradedEnemies.add(new UpgradedPirate(p));

        predictionManager = PredictionManager.getInstance();
    }

    public boolean carryBoiOffsideAfterSP(Pirate pirate){
        boolean anyOffside = false;
        // all enemies
        List<Pirate>enemies = upgradedEnemies.stream().map(upgradedPirate -> upgradedPirate.pirate).collect(Collectors.toList());
        // old locs to backup
        List<Location>oldLocs = enemies.stream().map(pirate1 -> pirate1.location).collect(Collectors.toList());
        // move the enemies to their new locations
        enemies.forEach(pirate1 -> pirate1.location = pirate1.location.towards(pirate,-game.superPushDistance));
        if (upgradedPirates.stream().anyMatch(upgradedPirate -> upgradedPirate.pirate.capsule!=null && isOffside(upgradedPirate.pirate)))
            anyOffside = true;

        for (int i=0;i<enemies.size();i++)
            enemies.get(i).location = oldLocs.get(i);

        return anyOffside;
    }

    public static BotData getInstance() {
        return instance;
    }

    //called once EVERY turn, updates all data
    public void update(PirateGame newGame) {
        this.game = newGame;

        initializeFlags();   // resets the flags


        //updates the location and direction of all upgraded pirates
        updateUpgradedPirates();

        clearAsteroidPushedCount();

        //initializes attacking pirates, all pirates that doesn't have a sticky bomb are attacking pirate at first
        initializeAttackingPirates();

        //initializes all friendly pirates with sticky bomb on them
        initializePiratesWithSticky();

        // creates units for the carry bois, removes the unit members from the attacking pirates list
        initializeUnits();

        //swap states of pirates that need to swap
        swapStates();

        detectShittyAssTactic(); //detect whether there is resting asteroid on our base

        predictionManager.makePredictions();  //update nextlocations of upgrandedEnmies according the prediction saved in prediction manager
    }

    private void initializeFlags() {
        Flags.ASTEROID_ON_BASE = false; //currently we are not aware about an asteroid on our base
        Flags.SWAPPED_THIS_TURN = false; //at the start of the turn swaps haven't happened yet for we need to know the pirates roles and movements before swapping
        Flags.SUPER_PUSHED = false;
    }

    //updates the location and direction of all upgraded pirates
    private void updateUpgradedPirates() {
        for (int i = 0; i < upgradedEnemies.size(); i++)
            upgradedEnemies.get(i).setPirate(game.getAllEnemyPirates()[i]);

        for (int i = 0; i < upgradedPirates.size(); i++)
            upgradedPirates.get(i).setPirate(game.getAllMyPirates()[i]);

        predictionManager.updateReliability();

        // update enemies - updates next and last locations
        for (UpgradedPirate umo : getEnemyUpgradedPirates())
            umo.updateDirectionAndLoc();
        for (UpgradedPirate up : getMyLivingPirates())
            up.updateDirectionAndLoc();

    }

    // reset the list of asteroids that was pushed this turn
    private void clearAsteroidPushedCount() {
        asteroidsPushedIds = new ArrayList<>();
    }

    // all the pirates that doesn't have a sticky are set as attackingPirates
    private void initializeAttackingPirates() {
        attackingPirates.clear();
        attackingPirates = Arrays.stream(game.getMyLivingPirates()).filter(pirate -> pirate.stickyBombs.length == 0).collect(Collectors.toList());
    }

    //all friendly pirates with sticky
    private void initializePiratesWithSticky() {
        piratesWithSticky = new ArrayList<>();

        for (Pirate p : game.getMyLivingPirates())
            if (p.stickyBombs.length > 0)
                piratesWithSticky.add(p);
    }

    // create 1 unit for each friendly carry-boi, then adds the closest attacking pirate to it
    private void initializeUnits() {

        units = new ArrayList<>();
        List<Pirate> carryBoys = new ArrayList<>();
        Iterator<Pirate> pirateIterator = attackingPirates.iterator();
        Pirate p;


        while (pirateIterator.hasNext()) { //going over all attacking pirates
            p = pirateIterator.next();

            if (p.hasCapsule()) { //if the pirate is a carry - boi create a unit around it
                Unit current = new Unit(p);
                units.add(current);
                if (!current.offside) // don't touch this
                    carryBoys.add(p);
                pirateIterator.remove();
            }
        }

        // join with carry boy if you are the closest to him and vice versa
        List<Pirate> currentCbs = new ArrayList<>(carryBoys);
        Pirate cb = null;
        pirateIterator = attackingPirates.iterator();


        while (pirateIterator.hasNext() && currentCbs.size() > 0) { //(supposedly) adds the best match of an attacker and a carry boi to a unit
            p = pirateIterator.next();

            for (Pirate carryBoy : currentCbs) {
                if (MapMethods.closest(carryBoy, attackingPirates).equals(p)) {
                    cb = MapMethods.closest(p, currentCbs);
                }
            }
            if (cb != null && getUnit(cb).size() < 2) {
                getUnit(cb).addPirate(p);
                currentCbs.remove(cb);
                pirateIterator.remove();
            }
        }
    }

    private void swapStates() {

        // replace a heavy UNIT member with an attacker if the member can't push and he doesn't have a capsule
        replaceHeavyUnitMemberWithNormalAttacker();

        //if leader is fat, and he is not threatened by 2 this turn or the next (assume going to his closest mothership) then turn him to be skinny again
        leaderGetNormal();

    }

    //if an asteroid is resting on one of our motherships, they are using shitty tactics
    private void detectShittyAssTactic() {
        for (Mothership mothership : game.getMyMotherships()) {
            for (Asteroid asteroid : game.getLivingAsteroids()) {
                if (asteroid.distance(mothership) < asteroid.size && asteroid.direction.equals(new Location(0, 0))) {
                    Flags.ASTEROID_ON_BASE = true;
                    astroOnOurBase = asteroid;
                }
            }
        }
    }

    public Unit getUnit(Pirate pirate) { //returns the unit containing this pirate
        for (Unit unit : units) {
            if (unit.contains(pirate))
                return unit;
        }
        return null;
    }

    private List<UpgradedPirate> getEnemyUpgradedPirates() {
        return upgradedEnemies.stream().filter(upgradedPirate -> upgradedPirate.pirate.isAlive()).collect(Collectors.toList());
    }

    public List<UpgradedPirate> getMyLivingPirates() {
        return upgradedPirates.stream().filter(upgradedPirate -> upgradedPirate.pirate.isAlive()).collect(Collectors.toList());
    }


    public <T> int indexOf(T[] arr, T n) {
        for (int i = 0; i < arr.length; i++)
            if (n == arr[i])
                return i;
        return -1;
    }

    //receives a pirate, returns him upgraded
    public UpgradedPirate upgraded(Pirate pirate) {
        for (UpgradedPirate upgradedPirate : upgradedEnemies)
            if (upgradedPirate.pirate.equals(pirate))
                return upgradedPirate;
        for (UpgradedPirate upgradedPirate : upgradedPirates)
            if (upgradedPirate.pirate.equals(pirate))
                return upgradedPirate;
        return null;
    }

    public void setGame(PirateGame game) {
        this.game = game;
    }

    public int max(List<Integer> arr) {
        int max = Integer.MIN_VALUE;
        for (int a : arr)
            max = a > max ? a : max;
        return max;
    }

    public int min(List<Integer> arr) {
        int min = Integer.MAX_VALUE;
        for (int a : arr)
            min = a < min ? a : min;
        return min;
    }


    // this method receives an object and whether it is ours and returns if it is offside, can receive a location
    public boolean isOffside(MapObject checkObj, boolean ourObject) {
        Mothership closestMs = MapMethods.closest(checkObj, ourObject ? game.getMyMotherships() : game.getEnemyMotherships());
        return MapMethods.canGoStraightAgainstSmartDefenseCareFully(checkObj, ourObject ? game.getEnemyLivingPirates() : game.getMyLivingPirates(), closestMs, 0);
    }

    public boolean isOffsideWithForTurns(MapObject checkObj, boolean ourObject, int forTurns) {
        Mothership closestMs = MapMethods.closest(checkObj, ourObject ? game.getMyMotherships() : game.getEnemyMotherships());
        return MapMethods.canGoStraightAgainstSmartDefenseCareFully(checkObj, ourObject ? game.getEnemyLivingPirates() : game.getMyLivingPirates(), closestMs, forTurns);
    }

    //is offside that gets a pirate
    public boolean isOffside(Pirate pirate) {
        boolean ourObject = pirate.owner.equals(game.getMyself()); //2deep4me
        return isOffside(pirate,ourObject);
    }

    //if leader is fat, and he is not threatened by 2 this turn or the next (assume going to his closest mothership) then turn him to be skinny again
    private void leaderGetNormal() {
        Iterator<Pirate> attackingPirateIterator;
        Pirate currentAttacker;

        for (Unit unit : units) {
            if (unit.leader.stateName.equals("heavy")) {
                attackingPirateIterator = attackingPirates.iterator();
                while (attackingPirateIterator.hasNext()) {
                    currentAttacker = attackingPirateIterator.next();
                    if (Sails.piratesThreatsOnLoc(unit.leader.location.towards(MapMethods.closest(unit.leader, game.getMyMotherships()), unit.leader.maxSpeed), game.getEnemyLivingPirates(), 200, 0) < 2) { // if the leader won't be threatened by 2
                        if (currentAttacker.stateName.equals("normal") && !Flags.SWAPPED_THIS_TURN && currentAttacker.stickyBombs.length == 0) {
                            currentAttacker.swapStates(unit.leader);
                            System.out.println(currentAttacker + " swaps with " + unit.leader + " to make leader in offside get normal");
                            Flags.SWAPPED_THIS_TURN = true;
                            attackingPirateIterator.remove(); // remove the attacker because he made his turn
                        }
                    }
                }
            }
        }
    }

    // replace a heavy UNIT member with an attacker if the member can't push and he doesn't have a capsule
    private void replaceHeavyUnitMemberWithNormalAttacker() {

        Iterator<Pirate> attackingPiratesIterator;
        Pirate attackingPirate;
        Location nextLeaderLoc, nextMemberLoc;

        for (Unit unit : units) { //for each unit 'unit'
            attackingPiratesIterator = attackingPirates.iterator();
            nextLeaderLoc = Sails.getCarefulSailLocation(unit.leader, unit.dest, unit.leader.maxSpeed);

            while (attackingPiratesIterator.hasNext() && !Flags.SWAPPED_THIS_TURN) { // find a normal attacking pirate that doesn't have a sticky
                attackingPirate = attackingPiratesIterator.next();

                if (attackingPirate.stateName.equals("normal") && attackingPirate.stickyBombs.length == 0) { //swaps the THICC unit member with the attacker
                    for (Pirate member : unit.getPiratesWithoutLeader()) {

                        nextMemberLoc = member.location.towards(nextLeaderLoc, member.maxSpeed);

                        //if the unit member is heavy, is not the leader and he will not be close enough to push, make him normal
                        if (member.stateName.equals("heavy") && member.capsule == null && nextLeaderLoc.distance(nextMemberLoc) > member.pushRange && !Flags.SWAPPED_THIS_TURN) {
                            attackingPirate.swapStates(member);
                            System.out.println(attackingPirate + " swaps with " + member + " to replace Heavy Unit Member With Normal attacker");
                            Flags.SWAPPED_THIS_TURN = true; // there can be only one swap each turn
                            attackingPiratesIterator.remove(); // remove the attacker because he made his turn
                        }
                    }
                }
            }
        }
    }

    //Here lies "ttySwapToCatchOffside", lived a short, useless life.  Rest In Peace.
    //for each hea pirate. / Out of context / how2lambda: pirate.blaballbabalbablabalbalb-->balba<--lbalbalbalbalbalbalba.collect.collctions().collect().annoyingshit().---><-----ooohaaah.Collect.Collections();
    //Out Of Context / pIraTE.iSOfFSiDe
}
