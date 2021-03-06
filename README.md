# intervalsets

Efficient immutable interval sets for [spire](https://github.com/non/spire)

[![Build Status](https://travis-ci.org/rklaehn/intervalset.png)](https://travis-ci.org/rklaehn/intervalset)

This repository contains two data structures for sets of *non-overlapping* intervals. Boundaries are either inclusive or exclusive, so (0..2] is different to [1..2]. 

## IntervalTrie

[IntervalTrie](IntervalTrie.md) is based on a binary TRIE and requires the element type to be convertable to a long while preserving order. This is the case for all primitive types.

## IntervalSeq

[IntervalSeq](IntervalSeq.md) is based on sorted sequences of boundary values and works for any element type for which an Order instance is available.

## QuickArrayMerge

An utility to merge two sorted arrays with a close to optimal number of comparisons
