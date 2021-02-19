package ch.sbb.matsim.routing.graph;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * @author mrieser / Simunto
 */
public class DAryMinHeapTest {

	@Test
	public void testPoll() {
		double cost[] = new double[10];
		DAryMinHeap pq = new DAryMinHeap(20, 3);

		cost[0] = 5;
		cost[1] = 2;
		cost[2] = 4;
		cost[3] = 8;
		cost[4] = 10;
		cost[5] = 1;
		cost[6] = 3;
		cost[7] = 6;

		pq.insert(2, cost[2]);
		pq.insert(1, cost[1]);
		pq.insert(0, cost[0]);

		Assert.assertEquals(3, pq.size());

		Assert.assertEquals(1, pq.poll());
		Assert.assertEquals(2, pq.poll());
		Assert.assertEquals(0, pq.poll());

		Assert.assertTrue(pq.isEmpty());

		for (int i = 0; i < 8; i++) {
			pq.insert(i, cost[i]);
		}

		Assert.assertEquals(5, pq.poll());
		Assert.assertEquals(1, pq.poll());
		Assert.assertEquals(6, pq.poll());
		Assert.assertEquals(2, pq.poll());
		Assert.assertEquals(0, pq.poll());
		Assert.assertEquals(7, pq.poll());
		Assert.assertEquals(3, pq.poll());
		Assert.assertEquals(4, pq.poll());
		Assert.assertTrue(pq.isEmpty());
	}

	@Test
	public void testDecreaseKey() {
		double cost[] = new double[10];
		DAryMinHeap pq = new DAryMinHeap(20, 4);

		cost[0] = 5;
		cost[1] = 2;
		cost[2] = 4;
		pq.insert(2, cost[2]);
		pq.insert(1, cost[1]);
		pq.insert(0, cost[0]);

		pq.decreaseKey(2, 1.0);
		cost[2] = 1.0;

		Assert.assertEquals(2, pq.poll());
		Assert.assertEquals(1, pq.poll());
		Assert.assertEquals(0, pq.poll());
		Assert.assertTrue(pq.isEmpty());
	}

	@Test
	public void stresstest_insert() {
		int cnt = 10000;
		double[] cost = new double[cnt];
		Random r = new Random(20190210L);
		for (int i = 0; i < cnt; i++) {
			cost[i] = (int) (r.nextDouble() * cnt * 100);
		}

		DAryMinHeap pq = new DAryMinHeap(cnt, 2);

		for (int i = 0; i < cnt; i++) {
			pq.insert(i, cost[i]);
		}
		double lastCost = -1;
		int step = 0;
		while (!pq.isEmpty()) {
			step++;
			int node = pq.poll();
			double nodeCost = cost[node];
			Assert.assertTrue(step + ": " + lastCost + " <= " + nodeCost, lastCost <= nodeCost);
			lastCost = nodeCost;
		}
	}

	@Test
	public void stresstest_decreaseKey() {
		int cnt = 10000;
		double[] cost = new double[cnt];
		Random r = new Random(20190218L);
		for (int i = 0; i < cnt; i++) {
			cost[i] = (int) (r.nextDouble() * cnt * 100);
		}

		DAryMinHeap pq = new DAryMinHeap(cnt, 6);

		// first insert, then decreaseKey
		for (int i = 0; i < cnt; i++) {
			pq.insert(i, cost[i]);
		}

		for (int i = 0; i < cnt; i++) {
			double newCost = (int) (r.nextDouble() * cnt * 100);
			if (newCost < cost[i]) {
				pq.decreaseKey(i, newCost);
				cost[i] = newCost;
			}
		}

		double lastCost = -1;
		int step = 0;
		while (!pq.isEmpty()) {
			step++;
			int node = pq.poll();
			double nodeCost = cost[node];
			Assert.assertTrue(step + ": " + lastCost + " <= " + nodeCost, lastCost <= nodeCost);
			lastCost = nodeCost;
		}
	}

	@Test
	public void stresstest_remove() {
		int cnt = 10000;
		double[] cost = new double[cnt];
		Random r = new Random(20190219L);
		for (int i = 0; i < cnt; i++) {
			cost[i] = (int) (r.nextDouble() * cnt * 100);
		}

		DAryMinHeap pq = new DAryMinHeap(cnt, 4);

		// first insert, then remove some
		for (int i = 0; i < cnt; i++) {
			pq.insert(i, cost[i]);
		}

		for (int i = 0; i < cnt; i++) {
			if (r.nextDouble() < 0.1) {
				pq.remove(i);
				cost[i] = Double.NaN;
			}
		}

		double lastCost = -1;
		int step = 0;
		while (!pq.isEmpty()) {
			step++;
			int node = pq.poll();
			double nodeCost = cost[node];
			Assert.assertTrue(step + ": " + lastCost + " <= " + nodeCost, lastCost <= nodeCost);
			lastCost = nodeCost;
		}
	}
}