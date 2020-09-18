NTCIR 15 WWW-3 Task REP run by KASYS

We add new function for feature extraction.
New features we can get are idf, LMIR.JM, LMIR.DIR, LIMR.ABS

feature extraction command
`./target/appassembler/bin/FeatureExtractorCli -collection www -topicreader www -index document_index -out output_file -qrel qrel_file -topic topics_file`
