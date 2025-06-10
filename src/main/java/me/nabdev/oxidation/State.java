package me.nabdev.oxidation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.json.JSONObject;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import me.nabdev.oxidation.util.SmartEventLoop;
import me.nabdev.oxidation.util.SmartTrigger;

/**
 * A state in a state machine.
 */
public abstract class State {
    /**
     * Information about a transition between states.
     * 
     * @param target    The state to transition to
     * @param source    The state to transition from
     * @param condition The condition to transition on
     * @param priority  The priority of this transition (lower is higher priority)
     * @param name      The name of this transition (for debugging/visualization)
     */
    public record TransitionInfo(State target, State source, BooleanSupplier condition, int priority, String name) {
    }

    /**
     * The parent state of this state. If this is null, this state is the root (or
     * improperly configured!).
     */
    public State parentState;

    /**
     * The event loop for this state. This is used to manage the timing and
     * execution
     * of actions within this state.
     */
    protected final SmartEventLoop loop = new SmartEventLoop();

    /**
     * The parameters for this state. This can be used to make reusable states that
     * can be configured.
     */
    protected final JSONObject parameters;

    Map<State, List<TransitionInfo>> transitions = new HashMap<>();
    List<TransitionInfo> entranceConditions = new ArrayList<>();
    List<State> children = new ArrayList<>();

    List<Supplier<Command>> startCommands = new ArrayList<>();
    List<Command> currentStartCommands = new ArrayList<>();

    private final StateMachineBase stateMachine;

    private boolean hasDefaultChild = false;
    String name = this.getClass().getSimpleName();

    /**
     * Create a new state under the given state machine.
     * 
     * @param stateMachine The state machine this state belongs to
     */
    public State(StateMachineBase stateMachine) {
        this.stateMachine = stateMachine;
        this.parameters = new JSONObject();
    }

    /**
     * Create a new state under the given state machine.
     * 
     * @param stateMachine The state machine this state belongs to
     * @param parameters   The parameters for this state, used to configure it
     *                     (can be used to make reusable states)
     */
    public State(StateMachineBase stateMachine, JSONObject parameters) {
        this.stateMachine = stateMachine;
        this.parameters = parameters;
    }

    /**
     * Add a transition to a different state on a condition.
     * 
     * @param state     The state to transition to
     * @param condition The condition to transition on
     * @param name      The name of this transition (for debugging/visualization)
     * @return This state
     */
    public State withTransition(State state, BooleanSupplier condition, String name) {
        return withTransition(state, condition, Integer.MAX_VALUE, name);
    }

    /**
     * Add a transition to a different state on a condition.
     * 
     * @param state     The state to transition to
     * @param condition The condition to transition on
     * @param priority  The priority of this transition
     * @param name      The name of this transition (for debugging/visualization)
     * @return This state
     */
    public State withTransition(State state, BooleanSupplier condition, int priority, String name) {
        return withTransition(new TransitionInfo(state, this, condition, priority, name));
    }

    /**
     * Add a transition to a different state on a condition. (Handled by parent
     * state)
     * 
     * @param transition The transition info to add
     * @return This state
     */
    public State withTransition(TransitionInfo transition) {
        if (this == stateMachine.rootState)
            throw new RuntimeException("You cannot add a transition to the root state");

        if (parentState != null)
            parentState.addTransition(this, transition);
        else {
            throw new RuntimeException(
                    "You cannot add a transition to a state that is not a child of another state. Did you forget to add this state as a child of the root? Transition added to: "
                            + getDeepName());
        }

        return this;
    }

    /**
     * Add a transition between a child state and another state.
     * 
     * @param before     The child state to transition from
     * @param transition The transition info to add
     * @return This state
     */
    public State addTransition(State before, TransitionInfo transition) {
        if (transitions.containsKey(before)) {
            transitions.get(before).add(transition);
        } else {
            List<TransitionInfo> list = new ArrayList<>();
            list.add(transition);
            transitions.put(before, list);
        }
        stateMachine.markDirty();
        return this;
    }

    /**
     * Configure mode transitions for this state.
     * 
     * @param disabled The state to transition to when disabled
     * @param teleop   The state to transition to when teleop is enabled
     * @param auto     The state to transition to when auto is enabled
     * @param test     The state to transition to when test is enabled
     * 
     * @return This state
     */
    public State withModeTransitions(State disabled, State teleop, State auto, State test) {
        if (disabled != this)
            withTransition(disabled, DriverStation::isDisabled, "Robot Disabled");
        if (teleop != this)
            withTransition(teleop, DriverStation::isTeleopEnabled, "Teleop Enabled");
        if (auto != this)
            withTransition(auto, DriverStation::isAutonomousEnabled, "Auto Enabled");
        if (test != this)
            withTransition(test, DriverStation::isTestEnabled, "Test Enabled");
        return this;
    }

    /**
     * Configure mode transitions for this state, using tele as the auto state.
     * 
     * @param disabled The state to transition to when disabled
     * @param teleop   The state to transition to when teleop is enabled
     * @param test     The state to transition to when test is enabled
     * 
     * @return This state
     */
    public State withModeTransitions(State disabled, State teleop, State test) {
        if (disabled != this)
            withTransition(disabled, DriverStation::isDisabled, "Robot Disabled");
        if (teleop != this)
            withTransition(teleop, DriverStation::isTeleopEnabled, "Teleop Enabled");
        if (teleop != this)
            withTransition(teleop, DriverStation::isAutonomousEnabled, "Auto Enabled");
        if (test != this)
            withTransition(test, DriverStation::isTestEnabled, "Test Enabled");
        return this;
    }

    /**
     * Add a child state to this state.
     *
     * @param child                 The child state
     * @param condition             The condition to transition to this state
     * @param priority              The priority of this state
     * @param entranceConditionName The name of the entrance condition for this
     *                              state (for debugging/visualization)
     * 
     * @return This state
     */
    public State withChild(State child, BooleanSupplier condition, int priority, String entranceConditionName) {
        addChild(child, condition, priority, false, entranceConditionName);
        return this;
    }

    /**
     * Add a child state to this state (will never be entered by default)
     * 
     * @param child The child state
     * @return This state
     */
    public State withChild(State child) {
        addChild(child, () -> false, Integer.MAX_VALUE, false, "impossible");
        return this;
    }

    /**
     * Add a default child state to this state.
     *
     * @param child The child state
     * @return This state
     */
    public State withDefaultChild(State child) {
        addChild(child, () -> true, Integer.MAX_VALUE, true, "default");
        return this;
    }

    /**
     * Remove all children from this state.
     * 
     * @return This state
     */
    public State withNoChildren() {
        transitions.clear();
        return this;
    }

    /**
     * Set the name of this state.
     * 
     * @param name The name to set
     * @return This state
     */
    public State withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the name of this state.
     * 
     * @return The name of this state
     */
    public String getName() {
        return name;
    }

    /**
     * Get the name of this state, including the names of all parent states.
     * 
     * @return The recurisive name of this state
     */
    public String getDeepName() {
        if (parentState == null)
            return getName();
        return parentState.getDeepName() + "/" + getName();
    }

    /**
     * Check if this state "is" another state, meaning it is the same state or a
     * child of that state.
     * 
     * @param state The state to check against
     * @return True if this state is the same as or a child of the given state,
     *         false otherwise
     */
    public boolean is(State state) {
        if (state == this)
            return true;

        if (parentState == null)
            return false;

        return parentState.is(state);
    }

    /**
     * Fires when the state is exited
     */
    public void onExit() {
        loop.stop();
        CommandScheduler.getInstance().cancel(currentStartCommands.toArray(new Command[0]));
        currentStartCommands.clear();
    }

    /**
     * Fires when the state is entered
     */
    public void onEnter() {
        for (Supplier<Command> commandSup : startCommands) {
            Command command = commandSup.get();
            if (command == null) {
                DriverStation.reportWarning("A command passed to startWhenActive was null", false);
                continue;
            }
            currentStartCommands.add(command);
            command.schedule();
        }
    };

    /**
     * Shorthand to create a SmartTrigger bound to this state's event loop.
     * 
     * @param condition The condition to give to the trigger
     * @return A SmartTrigger with the provided condition bound to this state's
     *         event loop
     */
    public SmartTrigger t(BooleanSupplier condition) {
        return new SmartTrigger(loop, condition);
    }

    /**
     * Run an action when the state is active
     * Cancels it if not already cancelled when the state is exited
     * 
     * @param cmd The command to run when the state is active
     */
    protected void startWhenActive(Command cmd) {
        startCommands.add(() -> cmd);
    }

    /**
     * Run an action when the state is active
     * Cancels it if not already cancelled when the state is exited
     * 
     * @param cmd The command supplier to poll and run when the state is active
     */
    protected void startWhenActive(Supplier<Command> cmd) {
        startCommands.add(cmd);
    }

    /**
     * WARNING - activeTrg does not experience a rising edge, so it will not fire!
     * Use only with runWhileTrue and runWhileFalse, or compositions.
     * 
     * @return A trigger that is active while this state is active
     */
    protected SmartTrigger activeTrg() {
        return new SmartTrigger(loop, () -> stateMachine.currentState.is(this));
    }

    void run() {
        if (parentState != null)
            parentState.run();

        loop.poll();
    }

    // boolean checkTransitions() {
    // if (parentState != null) {
    // boolean didParentTransition = parentState.checkTransitions();
    // if (didParentTransition)
    // return true;
    // }

    // TransitionInfo next = evaluateTransition(transitions);
    // if (next != null) {
    // stateMachine.transitionTo(next.state);
    // return true;
    // }
    // return false;
    // }

    record TransitionEvalResult(State finalState, List<TransitionInfo> transitions) {
    }

    TransitionEvalResult evalTransitions(Stack<State> nodesToSearch, List<TransitionInfo> transitionList) {
        State next = nodesToSearch.pop();
        if (next == this)
            return new TransitionEvalResult(this, transitionList);

        TransitionInfo transition = evaluateBestTransition(transitions.get(next));
        if (transition != null) {
            transitionList.add(transition);
            return stateMachine.traverseTransitions(transition.target(), transitionList);
        }
        if (nodesToSearch.isEmpty())
            nodesToSearch.push(next.evaluateEntranceState());

        return next.evalTransitions(nodesToSearch, transitionList);
    }

    static TransitionInfo evaluateBestTransition(List<TransitionInfo> transitions) {
        if (transitions == null)
            return null;
        TransitionInfo best = null;
        for (TransitionInfo i : transitions) {
            if ((best == null || best.priority > i.priority) && i.condition.getAsBoolean()) {
                best = i;
            }
        }
        return best;
    }

    State evaluateEntranceState() {
        if (entranceConditions.isEmpty())
            return this;
        TransitionInfo next = evaluateBestTransition(entranceConditions);
        if (next == null || next.target() == null)
            throw new RuntimeException(
                    "A state (" + getDeepName()
                            + ") was unable to determine which child to transition to. Consider adding a default state.");
        return next.target().evaluateEntranceState();
    }

    void setParentState(State parentState) {
        if (this.parentState != null)
            throw new RuntimeException("A state can only have one parent state");
        this.parentState = parentState;
    }

    private void addChild(State child, BooleanSupplier condition, int priority, boolean isDefault,
            String entranceConditionName) {
        if (isDefault) {
            if (hasDefaultChild)
                throw new RuntimeException("A state can only have one default child");
            hasDefaultChild = true;
        }
        child.setParentState(this);
        children.add(child);
        entranceConditions.add(new TransitionInfo(child, this, condition, priority, entranceConditionName));
        stateMachine.markDirty();
    }
}
