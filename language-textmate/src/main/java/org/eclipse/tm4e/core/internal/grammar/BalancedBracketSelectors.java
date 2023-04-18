/**
 * Copyright (c) 2022 Sebastian Thomschke and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Initial code from https://github.com/microsoft/vscode-textmate/
 * Initial copyright Copyright (C) Microsoft Corporation. All rights reserved.
 * Initial license: MIT
 *
 * Contributors:
 * - Microsoft Corporation: Initial code, written in TypeScript, licensed under MIT license
 * - Sebastian Thomschke - translation and adaptation to Java
 */
package org.eclipse.tm4e.core.internal.grammar;

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.tm4e.core.internal.matcher.Matcher;

/**
 * @see <a href=
 *      "https://github.com/microsoft/vscode-textmate/blob/e8d1fc5d04b2fc91384c7a895f6c9ff296a38ac8/src/grammar.ts#L808">
 *      github.com/microsoft/vscode-textmate/blob/main/src/grammar.ts</a>
 */
public class BalancedBracketSelectors {
	private final Matcher<List<String>>[] balancedBracketScopes;
	private final Matcher<List<String>>[] unbalancedBracketScopes;

	private boolean allowAny = false;

	public BalancedBracketSelectors(
		final List<String> balancedBracketScopes,
		final List<String> unbalancedBracketScopes) {
		this.balancedBracketScopes = balancedBracketScopes.stream()
			.flatMap(selector -> {
				if ("*".equals(selector)) {
					this.allowAny = true;
					return Stream.empty();
				}
				return Matcher.createMatchers(selector).stream().map(m -> m.matcher);
			})
			.toArray(Matcher[]::new);

		this.unbalancedBracketScopes = unbalancedBracketScopes.stream()
			.flatMap(selector -> Matcher.createMatchers(selector).stream().map(m -> m.matcher))
			.toArray(Matcher[]::new);
	}

	boolean matchesAlways() {
		return this.allowAny && this.unbalancedBracketScopes.length == 0;
	}

	boolean matchesNever() {
		return !this.allowAny && this.balancedBracketScopes.length == 0;
	}

	boolean match(final List<String> scopes) {
		for (final Matcher<List<String>> excluder : this.unbalancedBracketScopes) {
			if (excluder.matches(scopes)) {
				return false;
			}
		}

		for (final Matcher<List<String>> includer : this.balancedBracketScopes) {
			if (includer.matches(scopes)) {
				return true;
			}
		}
		return this.allowAny;
	}
}
