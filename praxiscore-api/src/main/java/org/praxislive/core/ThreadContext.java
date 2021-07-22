/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2021 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 * 
 *
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.core;

/**
 * An optional context available from the {@link Root} lookup, providing the
 * ability to query whether code is running on the Root thread, and to pass
 * tasks to invoke on the Root thread.
 */
public interface ThreadContext {

    /**
     * Check whether the current thread is the active thread executing inside a
     * Root update.
     *
     * @return current thread is in Root update
     */
    public boolean isInUpdate();

    /**
     * Check whether the current thread is the thread currently driving a Root
     * implementation. This is a looser check than {@link #isInUpdate()} where a
     * Root implementation may be driven by an external context - eg. UI event
     * thread. It will always return true when isInUpdate() returns true. This
     * method does not return true if a thread has been returned to a pool. As a
     * guide, it will return true if blocking the current thread would block the
     * Root from executing.
     *
     * @return current thread is Root executing thread
     */
    public boolean isRootThread();

    /**
     * Execute a task on the Root thread. The task should carefully check that
     * its state is still valid - eg. a component should check that it has not
     * been removed from the Root hierarchy.
     *
     * @param task task to be executed
     */
    public void invokeLater(Runnable task);

}
