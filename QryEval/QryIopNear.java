/**
 * Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 *  The Near operator for all retrieval models.
 */
public class QryIopNear extends QryIop {
    // The distance between words
    private int n;

    public QryIopNear(int n) {
        this.n = n;
    }

    /**
     *  Evaluate the query operator; the result is an internal inverted
     *  list that may be accessed via the internal iterators.
     *  @throws IOException Error accessing the Lucene index.
     */
    protected void evaluate() throws IOException {
        this.invertedList = new InvList(this.getField());

        if (args.size() < 2) {
            return;
        }

        int docId = Qry.INVALID_DOCID;
        while (true) {
            // All the document id should match
            // if we have next match
            if (this.docIteratorHasMatchAll(null)) {
                docId = this.args.get(0).docIteratorGetMatch();
            } else {
                // if we don't have next match document
                // All process done
                break;
            }

            // After finding the docId, iterate over the position
            // record all the location to positions
            List<Integer> positions = new ArrayList<Integer>();
            int size = this.args.size();

            // If only have one list, then there is no need to do

            int[] locs;
            QryIop[] qryIops;
            while (true) {
                // If any QryIopTerm reaches the end of position, break;
                Boolean breakFlag = false;
                for (int i = 0; i < size; i++) {
                    QryIop q_i = (QryIop) this.args.get(i);
                    if (!q_i.locIteratorHasMatch()) {
                        breakFlag = true;
                        break;
                    }
                }
                if (breakFlag) { break;}

                Boolean matchFlag = true;
                locs = new int[size];
                qryIops = new QryIop[size];
                for(int i = 0; i < size; i++) {
                    qryIops[i] = (QryIop) this.args.get(i);
                    locs[i] = qryIops[i].locIteratorGetMatch();
                }
                for(int i = 0; i < size - 1; i++) {
                    if (!( locs[i + 1] - locs[i] > 0 && locs[i + 1] - locs[i] <= n)) {
                        matchFlag = false;
                        break;
                    }
                }
                // If Match, record the position and advance all the pointers
                if (matchFlag) {
                    positions.add(locs[size - 1]);
                    for (int i = 0; i < size; i++) {
                        qryIops[i].locIteratorAdvance();
                    }
                } else {
                    // If not match. advance the smallest pointer
                    int minidx = minIdx(locs);
                    qryIops[minidx].locIteratorAdvance();
                }
            }


            if (positions.size() > 0) {
                this.invertedList.appendPosting(docId, positions);
            }
            for (int i = 0; i < this.args.size(); i++) {
                this.args.get(i).docIteratorAdvancePast(docId);
            }
        }
    }


    private int minIdx(int[] nums) {
        int mindidx = 0;
        for (int i = 1; i < nums.length; i++) {
            if (nums[mindidx] > nums[i]) {
                mindidx = i;
            }
        }
        return mindidx;
    }

}
