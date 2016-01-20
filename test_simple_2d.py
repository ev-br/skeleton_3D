from __future__ import division, print_function, absolute_import

import numpy as np
from numpy.testing import assert_equal

import matplotlib.pyplot as plt
import matplotlib.ticker as ticker

from skel import prepare_image, compute_thin_image


def check_skel(fname, viz=True):
    img = np.loadtxt('data/' + fname + '.txt', dtype=np.uint8)

    if viz:
        ax = _viz(img, **dict(marker='s', color='b', s=99, alpha=0.2))

    img1 = prepare_image(img)
    img1 = compute_thin_image(img1)

    # undo padding, convert to 2D
    img1_2d = img1[1:-1, 1:-1, 1:-1]
    img1_2d = img1_2d.squeeze()

    if viz:
        ax = _viz(img1_2d, ax, **dict(marker='o', color='r',
                                      s=80, alpha=0.7, label='us'))

    # compare to FIJI
    img_f = np.loadtxt('data/' + fname + '_fiji.txt', dtype=np.uint8)

    if viz:
        ax = _viz(img_f, ax, **dict(marker='o', color='g', s=45, label='fiji'))
    if viz:
        ax.legend()
        ax.grid(True)

        def yformatter(val, pos):
            return int(img.shape[1] - val + 1)
        def xformatter(val, pos):
            return int(val + 1)
        ax.xaxis.set_major_formatter(ticker.FuncFormatter(xformatter))
        ax.yaxis.set_major_formatter(ticker.FuncFormatter(yformatter))

        plt.show()


def _viz(img, ax=None, **kwds):

    if ax is None:
        import matplotlib.pyplot as plt
        fix, ax = plt.subplots()

    x, y = np.nonzero(img)
    ax.scatter(y, img.shape[1] - x, **kwds)

    return ax


if __name__ == "__main__":
#    check_skel('loop')
#    check_skel('cross')
    check_skel('two-hole')
#    check_skel('cross_crop4')
#    check_skel('strip')
