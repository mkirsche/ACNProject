import java.util.*;
public class TrafficSimulator {
public static void main(String[] args)
{
	initSeed();
	int numRequests = 1000000;
	int requestSize = 100;
	int spacing = 4;
	IncomingTraffic it = new IncomingTraffic(numRequests, requestSize, spacing);
	int numServers = 100;
	System.out.println("Parameters: " + numRequests + " " + requestSize + " " + spacing + " " + numServers);
	for(int i = 0; i<2; i++) simulate(it, numServers, i);
}
static void initSeed()
{
	r = new Random(135792468);
	seed = r.nextInt(987654321);
	seed2 = r.nextInt(987654321);
}
static Random r;
static int seed, seed2;
static int hash(int val, int s, int m)
{
	Random curr = new Random(s);
	long res = val;
	for(int i = 0; i<5; i++) res = (res ^ curr.nextInt());
	res %= m;
	if(res < 0) res += m;
	return (int)res;
}

static String[] schemeNames = {"Random", "Best of 2"};
/*
 * Schemes:
 * 0 = random
 * 1 = best of 2
 */
static void simulate(IncomingTraffic it, int numServers, int scheme)
{
	it.init();
	int numRequests = 0;
	int[] count = new int[numServers];
	while(!it.pq.isEmpty())
	{
		Packet cur = it.pq.poll();
		if(cur.extra == -1)
		{
			// Set extra bit
			if(scheme == 0)
			{
				cur.extra = 0;
			}
			else if(scheme == 1)
			{
				int hash1 = hash(cur.job, seed, numServers);
				int hash2 = hash(cur.job, seed2, numServers);
				if(count[hash1] > count[hash2]) cur.extra = 1;
				else cur.extra = 0;
			}
			numRequests += it.processSynAck(cur);
			continue;
		}
		// Hash the request number
		int hash = hash(cur.job, cur.extra == 0 ? seed : seed2, numServers);
		count[hash]++;
	}
	double expectedAverage = 1.0 * numRequests / numServers;
	int min = numRequests;
	int max = 0;
	for(int x : count)
	{
		min = Math.min(min, x);
		max = Math.max(max, x);
	}
	System.out.println("Simulated " + schemeNames[scheme]);
	System.out.println("Average: " + expectedAverage);
	System.out.println("Min: " + min);
	System.out.println("Max: " + max);
	double stdev = 0;
	for(int x: count) stdev += (x - expectedAverage) * (x - expectedAverage);
	stdev /= (numServers + 1);
	stdev = Math.sqrt(stdev);
	System.out.println("Standard Deviation: " + stdev);
	System.out.println();
}
static class IncomingTraffic
{
	PriorityQueue<Packet> pq = new PriorityQueue<Packet>();
	int numRequests; // How many requests are sent
	int requestSize; // Number of packets per request
	int spacing; // Time between requests
	IncomingTraffic(int nn, int rr, int ss)
	{
		numRequests = nn;
		requestSize = rr;
		spacing = ss;
	}
	int getSize(int requestSize)
	{
		return r.nextInt(2 * requestSize) + 1;
	}
	void init()
	{
		pq = new PriorityQueue<Packet>();
		int time = 0;
		for(int i = 0; i<numRequests; i++)
		{
			int extra = -1;
			pq.add(new Packet(time, r.nextInt(987654321), extra));
			time += spacing + r.nextInt(3) - 1;
		}
	}
	int processSynAck(Packet p)
	{
		int curSize = getSize(requestSize);
		for(int i = 0; i<curSize; i++)
		{
			pq.add(new Packet(p.time + i + 1, p.job, p.extra));
		}
		return curSize;
	}
}
static class Packet implements Comparable<Packet>
{
	int time;
	int job;
	int extra;
	
	Packet(int tt, int jj, int ee)
	{
		time = tt; job = jj; extra = ee;
	}

	public int compareTo(Packet o) {
		return time - o.time;
	}
}
}
