# kmap

## Overview

kmap is a hash-based mapper. For the moment kmap is Single-read only. 

## Download
As long kmap is not a complete package, kmap can only be downloaded as source code. 
This code has to be compiled before use.

## Background
I created kmap as an excercise to understand how mappers are working, starting from scratch.
Starting from scratch, multiple options arose: 

1. Reference indexing or Read indexing? As the reads are compaired with the reference, 
it seems logic to index the reference.

2. How to index? Using a Hash-table is a simple technique to find kmers in the reference. 
The problem with this indexing technique is that with big genomes, the memory use can become really high.

3. How to find the possible location of a read? As the reference is stored in a Hash-table, 
splitting the read in kmers, and find them is straight forward.

4. How to find mismatches and indels, when a possible location is found? By using a seed and extend 
approach, the found kmer is extended on both ends, while allowing possible mismatches and indels. 
Because I started from scratch, I implemented this logically. The performance of this part could be 
improved by using the Smith-Waterman algorithm.

5. How to calculate mapping quality? The chance that the read is the same as on the found possition 
on the reference follows the binomial probability. The chance is devided by the sumation of all 
probabilities of all possible mapping locations, with a maximum phred score of 60. (This follows 
more or less the BWA mapping quality calculation).

## Tools
kmap exists of 2 tools, the index and the map tool. The index tool creates an index of the 
reference genome for the map tool. While the map tool mappes reads against the index. The map tool 
has a possibility to map reads without first generating the index (ideal when multiple kmers are tested).

## Parameters
### index 
*    `-f`    path of the reference fasta file
*    `-kmer` the size of the kmer to use
*    `-gz`   the fasta file is gziped (true/false)
*    `-o`    output directory for the index files

### map
*    `-fastq`    the fastq file to map
*    `-gz`   the fastq file is gziped (true/false)
*    `-o`    the output directory
*    `-kmer` the used kmer for the index generation (only for use with `-reference`)
*    `-reference`    the path to the reference fasta file (index is generated before mapping, 
index is not writen to a drive)(`-kmer` option is needed, `-index` option is denied)
*    `-index`    the path to the index files(`-kmer` option is denied, `-reference` is not needed)
*    `-mapq`     the type of mapping quality: simple or bwalike (standard)

###Mapq types
*    `simple`    unique mapping read gets a score of 60, mapping on 2 places: 3, on 3 places: 2, on 9 or less: 1, else 0
*    `bwalike`   this is the standard. Here the score is a phred score of the probability. This is a binomial probability, 
where the number of bases equal to the reference is used as number of successes, the number of bases that are not clipped 
as the number of trials (matches, mismatches, indels), and the probability is 0.99 (base probability, the illumina error)  

##Examples
There is test data in the test_data folder. This folder contains a very small genome, and a small simulated
read file. The name of this read file also contains the expected cigar string.

Example of the index generation:

`java -jar kmap.jar index -reference reference.fa -kmer 22 -gz false -o .`

Examples of the mapping (with index, or with index generation):

`java -jar kmap.jar map -gz false -fastq test.fastq -o . -index .`

`java -jar kmap.jar map -gz false -fastq test.fastq -o . -reference chr22.part2.fa -kmer 15`