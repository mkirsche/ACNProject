"""
Code for generating a scatterplot
First three lines are plot title, x-axis label, y-axis label
Following lines are of form: x y [optional_category]
"""
import sys
import matplotlib.pyplot as plt

colors = ['purple', 'pink', 'cyan']

fn = sys.argv[1]
with open(fn) as f:
    lines = f.readlines()
    xs = []
    ys = []
    cs = []
    labels = []
    colormap = {}
    colorcount = 0
    for i in range(3, len(lines)):
        tokens = lines[i].split()
        xs.append(float(tokens[0]))
        ys.append(float(tokens[1]))
        if len(tokens) > 2:
            if(not tokens[2] in colormap):
                colormap[tokens[2]] = colorcount
                colorcount += 1
                labels.append(tokens[2])
            cs.append(colors[colormap[tokens[2]]])
        else:
            cs.append(colors[0])
    print(colorcount)
    if colorcount > 0:
        fig, ax = plt.subplots()
        for i in range(0, colorcount):
            curx = []
            cury = []
            for j in range(0, len(xs)):
                if cs[j] == colors[i]:
                    curx.append(xs[j])
                    cury.append(ys[j])
            print(colors[i]+' '+labels[i]+' '+str(cury))
            ax.scatter(curx, cury, c = colors[i], label = labels[i])
        plt.legend()
    else:
        print('no categories')
        plt.scatter(xs, ys, c = cs)
    plt.title(lines[0])
    plt.xlabel(lines[1])
    plt.ylabel(lines[2])
if(len(sys.argv) > 2):
    plt.savefig(sys.argv[2])
else:
    plt.show()
