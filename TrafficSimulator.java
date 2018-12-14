import java.util.*;
import java.io.*;
import org.apache.commons.math3.distribution.LogNormalDistribution;
public class TrafficSimulator {
	static boolean verbose = false;
	static int nonblockingUpdateFreq;
	static int numServers = 100;
	static double propTCP = 0.95;
	static int numTrials = 50;
	static IncomingTraffic it;
	static String updateFreqFile = "/home/mkirsche/github/ACNProject/updatefreq.txt";
	static String tcpPropFile = "/home/mkirsche/github/ACNProject/tcpprop.txt";
	static String tcpPropRangeFile = "/home/mkirsche/github/ACNProject/tcpproprange.txt";
	static String packetDelayFile = "/home/mkirsche/github/ACNProject/packetdelay.txt";
	static PrintWriter out, out2, pdout;
public static void main(String[] args) throws IOException
{
	initSeed();
	
	pdout = new PrintWriter(new File(packetDelayFile));
	pdout.println("Time Between Packets");
	
	int numRequests = 10000;
	int requestSize = 10;
	int spacing = 4;
	it = new IncomingTraffic(numRequests, requestSize, spacing, propTCP);
	System.out.println("Parameters: " + numRequests + " " + requestSize + " " + spacing + " " + numServers);
	//runBaseline();
	//testBestOf2Hashing();
	//runNonblockingTest();
	//testTCPProp();
	testMoreBits();
	pdout.close();
}
static void testTCPProp() throws IOException
{
	out = new PrintWriter(new File(tcpPropFile));
	out2 = new PrintWriter(new File(tcpPropRangeFile));
	
	out.println("Traffic Distributions - Standard Deviation");
	out.println("Proportion of TCP Traffic");
	out.println("Load standard deviation");
	
	out2.println("Traffic Distributions - Range");
	out2.println("Proportion of TCP Traffic");
	out2.println("Load range (max - min)");
	
	double tmp = propTCP;
	for(int scheme = 0; scheme < 2; scheme++)
	{
		for(int i = 0; i<=20; i++)
		{
			double cur = 0.05 * i;
			propTCP = cur;
			System.out.println("Set TCP Proportion to " + String.format("%.2f", propTCP));
			double[] res = simulate(it, numServers, scheme, propTCP, numTrials);
			double stdev = res[0];
			double range = res[2] - res[1];
			out.println(cur+" "+stdev+" "+schemeNames[scheme]);
			out2.println(cur+" "+range+" "+schemeNames[scheme]);
		}
	}
	propTCP = tmp;
	out.close();
	out2.close();
}
/*
 * Runs baseline algorithm
 */
static void runBaseline()
{
	simulate(it, numServers, 0, propTCP, numTrials);
}
/*
 * Compares deterministic hashing to adjacent to make sure second hash function is sufficiently random
 */
static void testBestOf2Hashing()
{
	for(int i= 1; i<=2; i++) simulate(it, numServers, i, propTCP, numTrials);
}
static void testMoreBits()
{
	int[] runs = new int[] {0, 1, 4, 5};
	for(int x : runs) simulate(it, numServers, x, propTCP, numTrials);
}
/*
 * Tests different update frequencies for "non-blocking" variant
 */
static void runNonblockingTest() throws IOException
{
	out = new PrintWriter(new File(updateFreqFile));
	out.println("Update Frequency");
	out.println("Update freqency (packets)");
	out.println("Load standard deviation");
	int tmp = nonblockingUpdateFreq;
	int[] nbfreqs = new int[] {50, 100, 250, 500, 750, 1000, 2000, 5000, 10000};
	for(int f : nbfreqs)
	{
		nonblockingUpdateFreq = f;
		System.out.println("Set update freq to " +  nonblockingUpdateFreq);
		double res = simulate(it, numServers, 3, propTCP, numTrials)[0];
		out.println(nonblockingUpdateFreq+" "+res);
	}
	nonblockingUpdateFreq = tmp;
	out.close();
}
static void initSeed()
{
	r = new Random(135792468);
	seeds = new int[8];
	for(int i = 0; i<seeds.length; i++) seeds[i] = r.nextInt(987654321);
}
static Random r;
static int[] seeds;
static int hash(int val, int s, int m)
{
	Random curr = new Random(s);
	long res = val;
	res ^= curr.nextInt();
	//for(int i = 0; i<20; i++) res = (res ^ curr.nextInt());
	res %= m;
	if(res < 0) res += m;
	return (int)res;
}

static boolean nonblocking(int scheme)
{
	return scheme == 3;
}

static String[] schemeNames = {"Random", "Best_of_2", "Best_of_2_adjacent", "Best_of_2_non-blocking", 
		"Best_of_4", "Best_of_8"};
/*
 * Schemes:
 * 0 = random
 * 1 = best of 2
 * 2 = best of 2 adjacent
 * 3 = best of 2 non-blocking
 * 4 = best of 4
 * 5 = best of 8
 */
static double[] simulate(IncomingTraffic it, int numServers, int scheme, double propTCP, int trials)
{
	double avgStdev = 0;
	double avgMin = 0;
	double avgMax = 0;
	double avgMean = 0;
	for(int tt = 0; tt<trials; tt++)
	{
		it.init();
		int numRequests = 0;
		int[] count = new int[numServers];
		int[] seenCount = new int[numServers];
		int[] countLastUpdated = new int[numServers];
		int countPseudotime = 0;
		while(!it.pq.isEmpty())
		{
			countPseudotime++;
			Packet cur = it.pq.poll();
			if(cur.extra == -1)
			{
				// Assign a traffic type as TCP or non-TCP
				boolean tcp = r.nextDouble() < propTCP;
				
				// Set extra bit
				if(scheme == 0 || !tcp)
				{
					cur.extra = 0;
				}
				else if(scheme == 1)
				{
					int hash1 = hash(cur.job, seeds[0], numServers);
					int hash2 = hash(cur.job, seeds[1], numServers);
					if(count[hash1] > count[hash2]) cur.extra = 1;
					else cur.extra = 0;
				}
				else if(scheme == 2)
				{
					int hash1 = hash(cur.job, seeds[0], numServers);
					int hash2 = (hash1 + 1)%numServers;
					if(count[hash1] > count[hash2]) cur.extra = 1;
					else cur.extra = 0;
				}
				else if(scheme == 3)
				{
					int hash1 = hash(cur.job, seeds[0], numServers);
					int hash2 = hash(cur.job, seeds[1], numServers);
					if(countPseudotime - countLastUpdated[hash1] > nonblockingUpdateFreq)
						seenCount[hash1] = count[hash1];
					if(countPseudotime - countLastUpdated[hash2] > nonblockingUpdateFreq)
						seenCount[hash2] = count[hash2];
					if(seenCount[hash1] > seenCount[hash2]) cur.extra = 1;
					else cur.extra = 0;
				}
				else if(scheme == 4)
				{
					int[] ids = new int[4];
					for(int i = 0; i<ids.length; i++) ids[i] = hash(cur.job, seeds[i], numServers);
					int[] counts = new int[ids.length];
					for(int i = 0; i<counts.length; i++) counts[i] = count[ids[i]];
					int best = 0;
					for(int i = 1; i<counts.length; i++)
					{
						if(counts[i] < counts[best]) best = i;
					}
					cur.extra = best;
				}
				else if(scheme == 5)
				{
					int[] ids = new int[8];
					for(int i = 0; i<ids.length; i++) ids[i] = hash(cur.job, seeds[i], numServers);
					int[] counts = new int[ids.length];
					for(int i = 0; i<counts.length; i++) counts[i] = count[ids[i]];
					int best = 0;
					for(int i = 1; i<counts.length; i++)
					{
						if(counts[i] < counts[best]) best = i;
					}
					cur.extra = best;
				}
				numRequests += it.processSynAck(cur);
				continue;
			}
			// Hash the request number
			int hash = hash(cur.job, seeds[cur.extra], numServers);
			if(scheme == 2) hash = hash(cur.job, seeds[0], numServers);
			if(scheme == 2 && cur.extra > 0) hash =  (hash + 1)%numServers;
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
		double stdev = 0;
		for(int x: count) stdev += (x - expectedAverage) * (x - expectedAverage);
		stdev /= (numServers + 1);
		stdev = Math.sqrt(stdev);
		if(verbose)
		{
			System.err.println("Simulated " + schemeNames[scheme] + " (trial " + (tt+1) + " of " + trials + ")");
			System.err.println("Average: " + expectedAverage);
			System.err.println("Min: " + min);
			System.err.println("Max: " + max);
			System.err.println("Standard Deviation: " + stdev);
			System.err.println();
		}
		
		avgMean += expectedAverage;
		avgStdev += stdev;
		avgMin += min;
		avgMax += max;
	}
	avgMean /= trials;
	avgStdev /= trials;
	avgMin /= trials;
	avgMax /= trials;
	System.out.println("Simulated " + schemeNames[scheme]);
	System.out.println("Average mean: " + avgMean);
	System.out.println("Average Standard Deviation: " + avgStdev);
	System.out.println("Average min: " + avgMin);
	System.out.println("Average max: " + avgMax);
	System.out.println("Average range: " + (avgMax - avgMin));
	System.out.println();
	return new double[] {avgStdev, avgMin, avgMax};
}
static class IncomingTraffic
{
	LogNormalDistribution lnd;
	PriorityQueue<Packet> pq = new PriorityQueue<Packet>();
	int numRequests; // How many requests are sent
	int requestSize; // Number of packets per request
	int spacing; // Time between requests
	double propTCP; // Proportion of traffic which is TCP;
	IncomingTraffic(int nn, int rr, int ss, double pt)
	{
		numRequests = nn;
		requestSize = rr;
		spacing = ss;
		lnd = new LogNormalDistribution(spacing, Math.sqrt(spacing));
		propTCP = pt;
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
			int delay = (int)(lnd.sample() + 0.5);
			while(delay < 1 || delay > spacing + 10 * Math.sqrt(spacing))
			{
				delay = (int)(lnd.sample() + 0.5);
			}
			pdout.println(delay);
			time += delay;
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
