// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz;

import java.util.Objects;

/**
 * Opaque identifier of a fuzz job — returned by {@link Runner#submit} and
 * accepted by {@link Runner#wait}, {@link Runner#stream}, and
 * {@link Runner#stop}.
 *
 * <p>This is a tiny record by design: the server allocates the run-id,
 * we only carry it around. Don't pattern-match on the {@code value()} —
 * the engine has different id schemes for HTTP-API runs vs CLI-local runs
 * and the SDK shouldn't tie itself to either.</p>
 */
public record JobId(String value) {
    public JobId {
        Objects.requireNonNull(value, "job id value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("job id must not be blank");
        }
    }
}
