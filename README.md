# clj-schulze

A Clojure implementation of the Schulze voting method.

## Usage

The `schulze-winner` function is probably the only one you’ll need:

```clojure
(use '[clj-schulze :only schulze-winner])

(let [candidates #{:a :b :c},
      ballots {[:a :b :c] 3,
               [:b :a :c] 3,
               [:b #{:a :c}] 1,
               [:c :b :a] 1}]
  (schulze-winner ballots candidates))

; returns :b
```

Just pass a list of candidates and a list of ballots to `schulze-winner` and it will return the winner or, if there is not a unique winner, a set consisting of the winners.

### Input formats

`candidates` is a set specifying the valid candidates, i.e. the entries which may appear in ballots. Each entry in this set must be a keyword. An example is

```clojure
(def candidates #{:a :b :c :d :e})
```

`ballots` is a map in which each key is a ballot and each corresponding value is the number of times that ballot occurs. Each ballot is a vector listing candidates from the most preferred (the first element of the vector) to the least preferred (the last element). For example, the vector

```clojure
[:a :b :c]
```

expresses that the voter prefers candidate a over candidate b, and both of these over candidate c. Ties can be expressed by putting more than one candidate in a set:

```clojure
[:a #{:b :c}]
```

expresses that the voter prefers candidate a over candidates b and c, but has no preference among b and c.

If any valid candidates are omitted from the ballot, it is assumed that (1) the voter has no preference between the omitted candidates, and (2) the voter prefers all listed candidates to all omitted candidates. Thus with the example set of candidates above, the preceding ballot is equivalent to

```clojure
[:a #{:b :c} #{:d :e}]
```

*Tip:* if you just have a list of ballots without frequencies, you can use `frequencies` to get it into the form required by `schulze-winner`.

## Implementation details

I wrote this library mostly as an exercise in Clojure, so I’ve cared more about writing idiomatic Clojure code than I have about making the code algorithmically efficient. (Since I’m a relative beginner, though, the code still isn’t necessarily as idiomatic as it could be.) The code is pretty well-documented; please feel free to fork it and make a pull request if you have any improvements.

### Voting details

This library doesn’t currently implement any tie-breaking algorithm.

As [suggested by Schulze](http://home.versanet.de/~chris1-schulze/schulze1.pdf), the strengths of pairwise links are measured by the number of winning votes.

I am not an expert in voting methods or algorithms. I release this code in the hope that someone will find it useful and instructive, but it has not been audited in any sense and is thus not a great choice in any serious application.

### Ballot processing details

There is some leeway in the format of the ballots that are submitted; this section describes the transformations that are automatically applied so you know what you do and don’t need to do to the ballots before you pass them to the `schulze-winner` function.

Before any processing, each ballot is converted to a canonical form through the following process:

1. All candidates not included in the ballot are added as shown in the example above.
2. Any empty sets (`#{}`) in the ballot are removed.
3. Any “bare” elements are put into single-element sets, so that the vector consists entirely of sets.

Given the example set of candidates above, the canonical form of the ballot

```clojure
[:a #{:b :e} #{} #{:d}]
```

is

```clojure
[#{:a} #{:b :e} #{:d} #{:c}]
```

No nesting of sets is permitted.

## Further reading

For more information on the Schulze method, see

* [“A New Monotonic, Clone-Independent, Reversal Symmetric, and Condorcet-Consistent Single-Winner Election Method”](http://home.versanet.de/~chris1-schulze/schulze1.pdf) by Markus Schulze
* [“Schulze method” on Wikipedia](http://en.wikipedia.org/wiki/Schulze_method)
* [Markus Schulze’s website on the Schulze method](http://m-schulze.webhop.net/)

## Project information

clj-schulze is developed by [Benjamin Esham](http://www.bdesham.info). The [project website](https://github.com/bdesham/clj-schulze) is hosted on GitHub.

Thanks go to the fine folks in #clojure for answering tons of questions!

### Release history

Beginning with version 1.0.0, this project will use the [Semantic Versioning](http://semver.org/) specification by Tom Preston-Werner.

* 0.9.2 (2011-06-17)

    Mark all functions except for `schulze-winner` as private. (Continue to test the now-private functions using a workaround.)

* 0.9.1 (2011-06-15)

    Refine everything and update documentation in preparation for public release.

* 0.9.0 (2011-06-14)

    First working version! The library passes a number of tests extracted from online Schulze-method examples.
	
* 2011-05-29

    Project started

### License

Copyright © 2011, Benjamin Esham. This software is released under the following version of the MIT license:

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following condition: the above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

**The software is provided “as is”, without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose and noninfringement. In no event shall the authors or copyright holders be liable for any claim, damages or other liability, whether in an action of contract, tort or otherwise, arising from, out of or in connection with the software or the use or other dealings in the software.**
