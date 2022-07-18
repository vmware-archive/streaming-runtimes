const udf = require('streaming-runtime-udf-aggregator');

// --------- UDF aggregation function --------
function aggregate(headers, user, results) {

  if (!results.has(user.fullName)) {
    // Add new empty user aggregate to the result map
    results.set(user.fullName, {
      from: headers.windowStartTime,
      to: headers.windowEndTime,
      name: user.fullName,
      totalScore: 0,
    });
  }

  // Increment user's score.
  let userAggregate = results.get(user.fullName);

  userAggregate.totalScore =
    Number.parseInt(userAggregate.totalScore) + Number.parseInt(user.score);
}

new udf.Aggregator(aggregate).start();
