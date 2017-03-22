/**
 *  Copyright (c) 2017, Carnegie Mellon University.  All Rights Reserved.
 */

import java.util.*;


public class TermScoreList {


  public PriorityQueue<Entry> queue;
  int limit;

  TermScoreList (int size) {
    queue  = new PriorityQueue<>(size, new Comparator<Entry>() {
      @Override
      public int compare(Entry o1, Entry o2) {
        if (o1.score == o2.score) {
          return 0;
        } else if (o1.score > o2.score) {
          return 1;
        }
        return -1;
      }
    });
    limit = size;
  }


  // queue should
  public void add(String term, double score) {
    if (queue.size() < limit) {
      queue.add(new Entry(term, score));
    } else {
      double minScore = queue.peek().score;
      if (score > minScore) {
        Entry polled = queue.poll();
        queue.add(new Entry(term, score));
      }
    }
  }


  public Entry getNext() {
    return queue.poll();
  }


  public Boolean hasNext() {
    if (queue != null && queue.size() > 0) {
      return true;
    }
    return false;
  }

//
//  public List<Entry> list = new ArrayList<Entry>();
//
//  public String getTerm(int index) {
//    return list.get(index).term;
//  }
//
//  public double getScore(int index) {
//    return list.get(index).score;
//  }
//
//
//  public void add(String term, double score) {
//    list.add(new Entry(term, score));
//  }
//
//  public int size() {
//    return this.list.size();
//  }
//
//  /*
//   *  Compare two Entry objects.  Sort by score, then
//   *  internal docid.
//   */
//  public class ScoreListComparator implements Comparator<Entry> {
//
//    @Override
//    public int compare(Entry s1, Entry s2) {
//      if (s1.score > s2.score)
//        return -1;
//      else if (s1.score < s2.score)
//        return 1;
//      else
//        return 0;
//    }
//
//  }
//
//  /**
//   *  Sort the list by score
//   */
//  public void sort () {
//    Collections.sort(this.list, new ScoreListComparator());
//  }
//
//  /**
//   * Reduce the score list to the first num results to save on RAM.
//   * @param num Number of results to keep.
//   */
//  public void truncate(int num) {
//    List<Entry> truncated = new ArrayList<Entry>(this.list.subList(0,
//        Math.min(num, list.size())));
//    this.list.clear();
//    this.list = truncated;
//  }
}
