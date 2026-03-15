function parseDurationToSeconds(raw) {
    if (!raw) {
        return null;
    }

    const match = String(raw).trim().match(/^(\d+)(ms|s|m|h)$/);
    if (!match) {
        return null;
    }

    const value = Number(match[1]);
    const unit = match[2];

    if (!Number.isFinite(value) || value <= 0) {
        return null;
    }

    if (unit === 'ms') {
        return Math.max(1, Math.ceil(value / 1000));
    }
    if (unit === 's') {
        return value;
    }
    if (unit === 'm') {
        return value * 60;
    }
    if (unit === 'h') {
        return value * 3600;
    }

    return null;
}

function formatSeconds(seconds) {
    const safe = Math.max(1, Math.round(seconds));

    if (safe % 3600 === 0) {
        return `${safe / 3600}h`;
    }
    if (safe % 60 === 0) {
        return `${safe / 60}m`;
    }
    return `${safe}s`;
}

function parsePositiveIntEnv(name, fallback) {
    const raw = __ENV[name];
    if (!raw) {
        return fallback;
    }

    const parsed = Number(raw);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function cloneOptions(options) {
    return JSON.parse(JSON.stringify(options));
}

function scaleStageDurations(stages, targetSeconds) {
    const seconds = stages
        .map((stage) => parseDurationToSeconds(stage.duration))
        .filter((value) => value !== null);

    const totalSeconds = seconds.reduce((sum, value) => sum + value, 0);
    if (totalSeconds <= 0) {
        return stages;
    }

    const exact = seconds.map((value) => (value / totalSeconds) * targetSeconds);
    const scaled = exact.map((value) => Math.max(1, Math.floor(value)));
    let allocated = scaled.reduce((sum, value) => sum + value, 0);

    while (allocated < targetSeconds) {
        let candidateIndex = 0;
        let bestFraction = -1;

        exact.forEach((value, index) => {
            const fraction = value - Math.floor(value);
            if (fraction > bestFraction) {
                bestFraction = fraction;
                candidateIndex = index;
            }
        });

        scaled[candidateIndex] += 1;
        allocated += 1;
    }

    while (allocated > targetSeconds) {
        let candidateIndex = -1;
        let largest = -1;

        scaled.forEach((value, index) => {
            if (value > 1 && value > largest) {
                largest = value;
                candidateIndex = index;
            }
        });

        if (candidateIndex === -1) {
            break;
        }

        scaled[candidateIndex] -= 1;
        allocated -= 1;
    }

    return stages.map((stage, index) => ({
        ...stage,
        duration: formatSeconds(scaled[index]),
    }));
}

function capNumber(value, cap) {
    if (!Number.isFinite(value)) {
        return value;
    }
    return Math.min(value, cap);
}

function setCappedProperty(target, key, cap) {
    if (!Object.prototype.hasOwnProperty.call(target, key)) {
        return;
    }

    const capped = capNumber(target[key], cap);
    if (capped === undefined) {
        delete target[key];
        return;
    }

    target[key] = capped;
}

function capStageTargets(scenario, vuCap, rateCap) {
    if (!Array.isArray(scenario.stages)) {
        return;
    }

    const isArrivalRate = scenario.executor && scenario.executor.includes('arrival-rate');
    scenario.stages = scenario.stages.map((stage) => ({
        ...stage,
        target: capNumber(stage.target, isArrivalRate ? rateCap : vuCap),
    }));
}

function applyQuickRunToScenario(scenario, targetSeconds, limits) {
    const nextScenario = { ...scenario };

    if (Array.isArray(nextScenario.stages) && nextScenario.stages.length > 0) {
        nextScenario.stages = scaleStageDurations(nextScenario.stages, targetSeconds);
    } else if (nextScenario.duration) {
        nextScenario.duration = formatSeconds(targetSeconds);
    }

    setCappedProperty(nextScenario, 'vus', limits.vuCap);
    setCappedProperty(nextScenario, 'startVUs', limits.vuCap);
    setCappedProperty(nextScenario, 'preAllocatedVUs', limits.preAllocatedVUsCap);
    setCappedProperty(nextScenario, 'maxVUs', limits.maxVUsCap);
    setCappedProperty(nextScenario, 'startRate', limits.rateCap);
    setCappedProperty(nextScenario, 'rate', limits.rateCap);

    capStageTargets(nextScenario, limits.vuCap, limits.rateCap);

    return nextScenario;
}

export function withQuickRunOptions(baseOptions) {
    const quickRunDuration = __ENV.QUICK_RUN_DURATION;
    if (!quickRunDuration) {
        return baseOptions;
    }

    const targetSeconds = parseDurationToSeconds(quickRunDuration);
    if (!targetSeconds) {
        return baseOptions;
    }

    const nextOptions = cloneOptions(baseOptions);
    const limits = {
        vuCap: parsePositiveIntEnv('QUICK_RUN_VU_CAP', 20),
        preAllocatedVUsCap: parsePositiveIntEnv('QUICK_RUN_PREALLOCATED_VUS_CAP', 100),
        maxVUsCap: parsePositiveIntEnv('QUICK_RUN_MAX_VUS_CAP', 300),
        rateCap: parsePositiveIntEnv('QUICK_RUN_RATE_CAP', 50),
    };

    setCappedProperty(nextOptions, 'vus', limits.vuCap);

    if (nextOptions.duration && !nextOptions.scenarios) {
        nextOptions.duration = formatSeconds(targetSeconds);
    }

    if (nextOptions.scenarios) {
        Object.keys(nextOptions.scenarios).forEach((name) => {
            nextOptions.scenarios[name] = applyQuickRunToScenario(
                nextOptions.scenarios[name],
                targetSeconds,
                limits
            );
        });
    }

    return nextOptions;
}
