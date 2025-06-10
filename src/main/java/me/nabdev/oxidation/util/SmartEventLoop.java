// Adapted from wpilib EventLoop class
// Copyright (c) 2009-2024 FIRST and other WPILib contributors All rights reserved.

package me.nabdev.oxidation.util;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;

import edu.wpi.first.wpilibj.event.EventLoop;
import edu.wpi.first.wpilibj2.command.Command;

/**
 * A modified {@link EventLoop} that allows for binding actions to be run
 * when the loop is polled, and allows for commands to be cancelled when the
 * loop is stopped.
 */
public final class SmartEventLoop {
    private final Collection<Runnable> m_bindings = new LinkedHashSet<>();
    private final Collection<Command> m_commands = new LinkedHashSet<>();
    private boolean m_running;

    /**
     * Bind a new action to run when the loop is polled.
     *
     * @param command the command to cancel when the loop is stopped.
     * @param action  the action to run.
     */
    public void bind(Command command, Runnable action) {
        if (m_running) {
            throw new ConcurrentModificationException("Cannot bind SmartEventLoop while it is running");
        }
        m_commands.add(command);
        m_bindings.add(action);
    }

    /** Poll all bindings. */
    public void poll() {
        m_running = true;
        m_bindings.forEach(Runnable::run);
    }

    /** Clear all bindings. */
    public void clear() {
        if (m_running) {
            throw new ConcurrentModificationException("Cannot clear SmartEventLoop while it is running");
        }
        m_bindings.clear();
        m_commands.clear();
    }

    /** Stops the event loop and cancels all associated commands. */
    public void stop() {
        m_running = false;
        for (Command c : m_commands) {
            c.cancel();
        }
    }

}
