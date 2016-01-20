from __future__ import division, print_function, absolute_import

import numpy as np
from numpy.testing import assert_equal

from skel import prepare_image, compute_thin_image

import matplotlib.pyplot as plt
import matplotlib.ticker as ticker


def check_skel(fname, viz=False):
    # compute the thin image and compare the result to that of ImageJ
    img = np.loadtxt('data/' + fname + '.txt', dtype=np.uint8)

    if viz:
        ax = _viz(img, **dict(marker='s', color='b', s=99, alpha=0.2))

    # compute
    img1 = prepare_image(img)
    img1 = compute_thin_image(img1)

    # undo padding, convert to 2D
    img1_2d = img1[1:-1, 1:-1, 1:-1]
    img1_2d = img1_2d.squeeze()
    img1_2d *= 255

    if viz:
        ax = _viz(img1_2d, ax, **dict(marker='o', color='r',
                                      s=80, alpha=0.7, label='us'))

    # compare to FIJI
    img_f = np.loadtxt('data/' + fname + '_fiji.txt', dtype=np.uint8)

    if not viz:
        # actually compare images
        assert_equal(img1_2d, img_f)
    else:
        ax = _viz(img_f, ax, **dict(marker='o', color='g', s=45, label='fiji'))

        ax.legend()
        ax.grid(True)

        def yformatter(val, pos):
            return int(img.shape[1] - val + 1)
        def xformatter(val, pos):
            return int(val + 1)
        ax.xaxis.set_major_formatter(ticker.FuncFormatter(xformatter))
        ax.yaxis.set_major_formatter(ticker.FuncFormatter(yformatter))

        plt.show()


# nose test generator
def test_simple_2d_images():
    for fname in ("strip", "loop", "cross", "two-hole"):
        yield check_skel, fname


def _viz(img, ax=None, **kwds):
    if ax is None:
        import matplotlib.pyplot as plt
        fix, ax = plt.subplots()

    x, y = np.nonzero(img)
    ax.scatter(y, img.shape[1] - x, **kwds)
    return ax


if __name__ == "__main__":
    import sys
    if len(sys.argv) < 2:
        sys.exit("Expect an image name from the data/ directory.")
    check_skel(sys.argv[1], True)
