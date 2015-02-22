package edu.berkeley.cs.succinct;

import edu.berkeley.cs.succinct.regex.parser.RegExParsingException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SuccinctIndexedBuffer extends SuccinctBuffer {

    protected static byte RECORD_DELIM = '\n';
    protected long[] offsets;

    /**
     * Constructor to initialize SuccinctIndexedBuffer from input byte array, offsets corresponding to records, and
     * context length.
     *  
     * @param input The input byte array.
     * @param offsets Offsets corresponding to records.
     * @param contextLen Context Length.
     */
    public SuccinctIndexedBuffer(byte[] input, long[] offsets, int contextLen) {
        super(input, contextLen);
        this.offsets = offsets;
    }

    /**
     * Constructor to initialize SuccinctIndexedBuffer from input byte array and offsets corresponding to records
     *
     * @param input The input byte array.
     * @param offsets Offsets corresponding to records.
     */
    public SuccinctIndexedBuffer(byte[] input, long[] offsets) {
        this(input, offsets, 3);
    }


    /**
     * Get the record delimiter.
     *
     * @return The record delimiter.
     */
    public static byte getRecordDelim() {
        return RECORD_DELIM;
    }

    /**
     * Search for offset corresponding to a position in the input.
     *
     * @param pos Position in the input
     * @return Offset corresponding to the position.
     */
    private int searchOffset(long pos) {
        int sp = 0, ep = offsets.length - 1;
        int m;

        while (sp <= ep) {
            m = (sp + ep) / 2;
            if (offsets[m] == pos) {
                return m;
            } else if (pos < offsets[m]) {
                ep = m - 1;
            } else {
                sp = m + 1;
            }
        }

        return ep;
    }

    /**
     * Search for an input query and return offsets of all matching records.
     *
     * @param query Input query.
     * @return Offsets of all matching records.
     */
    public Long[] recordSearchOffsets(byte[] query) {
        Set<Long> results = new HashSet<Long>();
        Pair<Long, Long> range;
        range = getRange(query);

        long sp = range.first, ep = range.second;
        if (ep - sp + 1 <= 0) {
            return new Long[0];
        }

        for (long i = 0; i < ep - sp + 1; i++) {
            results.add(offsets[searchOffset(lookupSA(sp + i))]);
        }

        return results.toArray(new Long[results.size()]);
    }

    /**
     * Search for all records that contains the query.
     *
     * @param query Input query.
     * @return All records containing input query.
     */
    public byte[][] recordSearch(byte[] query) {
        Set<Long> offsetResults = new HashSet<Long>();
        ArrayList<byte[]> results = new ArrayList<byte[]>();
        Pair<Long, Long> range;
        range = getRange(query);

        long sp = range.first, ep = range.second;
        if (ep - sp + 1 <= 0) {
            return new byte[0][0];
        }

        for (long i = 0; i < ep - sp + 1; i++) {
            long saVal = lookupSA(sp + i);
            int offsetIdx = searchOffset(saVal);
            long offset = offsets[offsetIdx];
            if(!offsetResults.contains(offset)) {
                results.add(extractUntil((int) offset, RECORD_DELIM));
                offsetResults.add(offset);
            }

        }

        return results.toArray(new byte[results.size()][]);
    }

    /**
     * Count of all records containing a particular query.
     *
     * @param query Input query.
     * @return Count of all records containing input query.
     */
    public long recordCount(byte[] query) {
        return recordSearchOffsets(query).length;
    }

    /**
     * Extract a part of all records.
     *
     * @param offset Offset into record.
     * @param length Length of part to be extracted.
     * @return Extracted data.
     */
    public byte[][] extractRecords(int offset, int length) {
        byte[][] records = new byte[offsets.length][];
        for(int i = 0; i < records.length; i++) {
            long curOffset = offsets[i] + offset;
            long nextOffset = (i == records.length - 1) ? getOriginalSize() : offsets[i + 1];
            if(length < nextOffset - curOffset)
                records[i] = extract((int) curOffset, length);
            else
                records[i] = extractUntil((int) curOffset, RECORD_DELIM);
        }
        return records;
    }

    /**
     * Search for all records that contain a particular regular expression.
     *
     * @param query The regular expression (UTF-8 encoded).
     * @return The records that contain the regular search expression.
     * @throws RegExParsingException
     */
    public byte[][] recordSearchRegex(String query) throws RegExParsingException {
        Map<Long, Integer> regexOffsetResults = regexSearch(query);
        Set<Long> offsetResults = new HashSet<Long>();
        ArrayList<byte[]> results = new ArrayList<byte[]>();
        for(Long offset: regexOffsetResults.keySet()) {
            int offsetIdx = searchOffset(offset);
            long recordOffset = offsets[offsetIdx];
            if(!offsetResults.contains(recordOffset)) {
                results.add(extractUntil(offset.intValue(), RECORD_DELIM));
                offsetResults.add(offset);
            }
        }
        return results.toArray(new byte[results.size()][]);
    }
}
