import numpy as np
import matplotlib.pyplot as plt

from skel import prepare_image, compute_thin_image


def get_rhombus(n=64, L=22, width=7):
    img = np.zeros((n, n), dtype=np.int8)

    x = np.arange(L, dtype=int)
    y = L - x

    for w in range(width):
        img[x + n//2, y + n//2 + w] = 1
        img[-x + n//2, y + n//2 + w] = 1
        img[x + n//2, -y + n//2 + w] = 1
        img[-x + n//2, -y + n//2 + w] = 1

    return img

def get_strip(n=64, width=3):
    img = np.zeros((n, n), dtype=np.int8)
    img[n//2 - width : n//2 + width, 2 : -2] = 1
    return img


if __name__ == "__main__":

    img = get_rhombus(n=24, L=8, width=4)

    x, y = np.nonzero(img)
    plt.scatter(x, y, marker='s', color='b')

#    # skeletonize
    img1 = prepare_image(img)
    img1 = compute_thin_image(img1)

    img1_2d = img1[1, :, :]
    x, y = np.nonzero(img1_2d)

    plt.scatter(x, y, marker='s', color='r')
    plt.show()
