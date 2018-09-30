//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package de.esoco.coroutine.step;

import de.esoco.coroutine.Continuation;
import de.esoco.coroutine.Coroutine;
import de.esoco.coroutine.CoroutineException;
import de.esoco.coroutine.CoroutineStep;
import de.esoco.coroutine.Suspension;

import de.esoco.lib.datatype.Pair;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.obrel.type.StandardTypes;

import static org.obrel.type.StandardTypes.DURATION;


/********************************************************************
 * A suspending {@link Coroutine} step that performes delayed executions.
 *
 * @author eso
 */
public class Delay<T> extends CoroutineStep<T, T>
{
	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param nDuration The duration to pause
	 * @param eUnit     The time unit of the duration
	 */
	public Delay(long nDuration, TimeUnit eUnit)
	{
		if (nDuration < 0)
		{
			throw new IllegalArgumentException("Duration must be >= 0");
		}

		Objects.requireNonNull(eUnit);

		set(DURATION, Pair.of(nDuration, eUnit));
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Suspends the coroutine execution for a certain time in milliseconds.
	 *
	 * @param nMilliseconds The milliseconds to sleep
	 *
	 * @see   #sleep(int, TimeUnit)
	 */
	public static <T> Delay<T> sleep(int nMilliseconds)
	{
		return sleep(nMilliseconds, TimeUnit.MILLISECONDS);
	}

	/***************************************
	 * Suspends the coroutine execution for a certain time. The configured
	 * duration can be overridden in the current continuation by setting the
	 * state relation {@link StandardTypes#DURATION}.
	 *
	 * @param  nDuration The duration to sleep
	 * @param  eUnit     The time unit of the duration
	 *
	 * @return A new step instance
	 */
	public static <T> Delay<T> sleep(int nDuration, TimeUnit eUnit)
	{
		return new Delay<>(nDuration, eUnit);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public T execute(T rInput, Continuation<?> rContinuation)
	{
		try
		{
			Pair<Long, TimeUnit> rDuration = getDuration(rContinuation);

			rDuration.second().sleep(rDuration.first());
		}
		catch (InterruptedException e)
		{
			throw new CoroutineException(e);
		}

		return rInput;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void runAsync(CompletableFuture<T> fPreviousExecution,
						 CoroutineStep<T, ?>  rNextStep,
						 Continuation<?>	  rContinuation)
	{
		Suspension<T> rSuspension = rContinuation.suspend(rNextStep);

		fPreviousExecution.thenAcceptAsync(
			i ->
			{
				Pair<Long, TimeUnit> rDuration = getDuration(rContinuation);

				rContinuation.context()
				.getScheduler()
				.schedule(
					() -> rSuspension.resume(i),
					rDuration.first(),
					rDuration.second());
			},
			rContinuation);
	}

	/***************************************
	 * Returns the duration from either the current continuation or, if not set,
	 * from this step.
	 *
	 * @param  rContinuation The continuation
	 *
	 * @return The duration
	 */
	private Pair<Long, TimeUnit> getDuration(Continuation<?> rContinuation)
	{
		Pair<Long, TimeUnit> rDuration =
			rContinuation.getState(DURATION, get(DURATION));

		return rDuration;
	}
}