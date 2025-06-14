package me.nabdev.oxidation;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;
import me.nabdev.oxidation.State.TransitionEvalResult;
import me.nabdev.oxidation.State.TransitionInfo;

/**
 * The base class for a state machine.
 * <p>
 * To use this class, create a subclass and states and transitions in
 * the constructor.
 */
public abstract class StateMachineBase {
    private static NetworkTable table = NetworkTableInstance.getDefault().getTable("StateMachine");
    private static NetworkTableEntry treeEntry = table.getEntry("Tree");
    private static NetworkTableEntry currentStateEntry = table.getEntry("CurrentState");
    private static NetworkTableEntry lastTransitionsEntry = table.getEntry("LastTransitions");
    /**
     * The root state of the state tree. This state will always be active, and all
     * other states will be children of this state.
     */
    public State rootState = new State(this) {
        {
            name = "Root";
        }
    };

    /**
     * The current active leaf state of the state tree.
     */
    public State currentState;

    private String currentTree = "";
    private boolean treeDirty = false;

    /**
     * Execute the state machine
     */
    public void periodic() {
        if (currentState == null) {
            DriverStation.reportWarning("The state machine has not been given an initial state, so it is useless!",
                    null);
            return;
        }
        List<TransitionInfo> lastTransitions = checkTransitions();
        currentState.run();
        if (currentState != null) {
            currentStateEntry.setString(currentState.getDeepName());
        } else {
            currentStateEntry.setString("null");
        }
        treeEntry.setString(getTree());
        if (lastTransitions.size() > 0) {
            JSONArray transitions = new JSONArray();
            for (TransitionInfo transition : lastTransitions) {
                transitions
                        .put(transition.name() + transition.target().getDeepName() + transition.source().getDeepName());
            }
            lastTransitionsEntry.setString(transitions.toString());
        }
    }

    /**
     * Registers a state as a child of the root state of the state machine.
     * 
     * @param state the state to register
     */
    public void registerToRootState(State... state) {
        for (State s : state) {
            rootState.children.add(s);
            s.setParentState(rootState);
        }
        markDirty();
    }

    /**
     * Called when the state machine is started.
     */
    public void onStartup() {
        currentState.onEnter();
    }

    private String getTree() {
        if (treeDirty) {
            currentTree = getObjectForState(rootState).toString();
            treeDirty = false;
        }
        return currentTree;
    }

    private List<TransitionInfo> checkTransitions() {
        TransitionEvalResult transitionEvalResult = traverseTransitions(currentState, new ArrayList<TransitionInfo>());
        State newState = transitionEvalResult.finalState();
        if (newState != currentState) {
            Stack<State> before = getStateTree(currentState);
            Stack<State> after = getStateTree(newState);

            while (!before.isEmpty() && !after.isEmpty() && before.peek() == after.peek()) {
                before.pop();
                after.pop();
            }

            Stack<State> exitStack = new Stack<>();
            while (!before.isEmpty()) {
                exitStack.push(before.pop());
            }
            while (!exitStack.isEmpty()) {
                exitStack.pop().onExit();
            }

            while (!after.isEmpty()) {
                after.pop().onEnter();
            }

            currentState = newState;
        }
        return transitionEvalResult.transitions();
    }

    void markDirty() {
        treeDirty = true;
    }

    TransitionEvalResult traverseTransitions(State state, List<TransitionInfo> transitions) {
        return rootState.evalTransitions(getStateTree(state), transitions);
    }

    Stack<State> getStateTree(State state) {
        Stack<State> stateTree = new Stack<>();
        State cur = state;
        while (cur.parentState != null) {
            stateTree.push(cur);
            cur = cur.parentState;
        }
        return stateTree;
    }

    JSONObject getObjectForState(State state) {
        JSONObject obj = new JSONObject();
        obj.put("name", state.getDeepName());
        JSONArray children = new JSONArray();
        for (State child : state.children) {
            children.put(getObjectForState(child));
        }
        obj.put("children", children);
        obj.put("parameters", state.parameters);
        if (state.parentState == null || !state.parentState.transitions.containsKey(state))
            return obj;
        JSONArray transitions = new JSONArray();
        for (TransitionInfo transition : state.parentState.transitions.get(state)) {
            JSONObject transitionObj = new JSONObject();
            transitionObj.put("name", transition.name());
            transitionObj.put("target", transition.target().getDeepName());
            transitions.put(transitionObj);
        }
        obj.put("transitions", transitions);

        JSONArray entranceConditions = new JSONArray();
        for (TransitionInfo entranceCondition : state.entranceConditions) {
            JSONObject transitionObj = new JSONObject();
            transitionObj.put("name", entranceCondition.name());
            transitionObj.put("target", entranceCondition.target().getDeepName());
            entranceConditions.put(transitionObj);
        }
        obj.put("entranceConditions", entranceConditions);

        return obj;

    }
}
