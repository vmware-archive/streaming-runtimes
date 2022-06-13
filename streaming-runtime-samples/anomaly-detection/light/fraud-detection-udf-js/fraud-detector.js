const udf = require('streaming-runtime-udf-aggregator');

// --------- UDF aggregation function --------
function aggregate(headers, authorization, results) {

    if (!results.has(authorization.card_number)) {

        results.set(authorization.card_number, {
            from: headers.windowStartTime,
            to: headers.windowEndTime,
            card_number: authorization.card_number,
            count: 0,
        });
    }

    // Increment the authorization count for that card_number.
    let authorizationCounter = results.get(authorization.card_number);
    authorizationCounter.count = Number.parseInt(authorizationCounter.count) + 1;
}

// --------- UDF release results function --------
function release(results) {

    let finalResults = new Map();

    // Filter in only the aggregates with more than 5 authorization attempts.    
    results.forEach((authorizationCounter, card_number) => {
        if (authorizationCounter.count > 5) {
            finalResults.set(card_number, authorizationCounter);
        }
    });

    return finalResults;
}

new udf.Aggregator(aggregate, release).start();