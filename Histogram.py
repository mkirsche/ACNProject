"""
Code for generating a histogram

First line is title
Each following line has a data point

"""
import sys
import matplotlib.pyplot as plt
import numpy as np

fn = sys.argv[1]
with open(fn) as f:
    lines = f.readlines()
    data = []
    maxval = 0
    for i in range(1, len(lines)):
        data.append(float(lines[i]))
        if float(lines[i]) > maxval:
            maxval = float(lines[i])
    mv = int(maxval + 1)
    step = max(1, int(maxval/50.0))
    plt.hist(data, bins = np.arange(0, int(maxval + 1), step))
    plt.title(lines[0])

if len(sys.argv) > 2:
    plt.savefig(sys.argv[2])
else:
    plt.show()
