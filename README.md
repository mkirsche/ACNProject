# Load Balancing TCP Traffic in Cloud Systems

To perform a simulation, compile and run TrafficSimulator.java (has dependencies on org.apache.commons.math3.distribution.LogNormalDistribution), which can be downloaded from Apache and added to an Eclipse project.  Alternately, the lognormal packet distribution can be removed from the code and replaced with something simpler like a uniform distribution.

The parameters can be changed at the beginning of the .java file before compiling, and are currently set to the values used in the paper.

Running the simulation will output statistics about each run, as well as produce output files which can be used to generate figures with "./FigGen.sh", and the resulting figures can have features such as their scale and axis labels changed in Scatterplot.py and Histogram.py.


