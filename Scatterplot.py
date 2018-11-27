"""
Code for generating a scatterplot
First three lines are plot title, x-axis label, y-axis label
Following lines are of form: x y
"""
import sys
import matplotlib.pyplot as plt

fn = sys.argv[1]
with open(fn) as f:
    lines = f.readlines()
    xs = []
    ys = []
    for i in range(3, len(lines)):
        tokens = lines[i].split()
        xs.append(float(tokens[0]))
        ys.append(float(tokens[1]))
    plt.scatter(xs, ys)
    plt.title(lines[0])
    plt.xlabel(lines[1])
    plt.ylabel(lines[2])
if(len(sys.argv) > 2):
    plt.savefig(sys.argv[2])
else:
    plt.show()
