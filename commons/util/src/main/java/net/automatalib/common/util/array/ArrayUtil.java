/* Copyright (C) 2013-2025 TU Dortmund University
 * This file is part of AutomataLib <https://automatalib.net>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.automatalib.common.util.array;

import java.util.Iterator;

/**
 * Utility methods for arrays.
 */
public final class ArrayUtil {

    private ArrayUtil() {
        // prevent instantiation
    }

    /**
     * A convenient method for calling {@code computeNewCapacity(length, requiredCapacity, 0)}.
     *
     * @param length
     *         the current length of the array
     * @param requiredCapacity
     *         the immediately required capacity
     *
     * @return the size of an array that is guaranteed to hold {@code requiredCapacity} number of elements.
     *
     * @see #computeNewCapacity(int, int, int)
     */
    public static int computeNewCapacity(int length, int requiredCapacity) {
        return computeNewCapacity(length, requiredCapacity, 0);
    }

    /**
     * Computes the size of an array that is required to hold {@code requiredCapacity} number of elements.
     * <p>
     * This method first tries to increase the size of the array by a factor of 1.5 to prevent a sequence of successive
     * increases by 1. It then evaluates the {@code nextCapacityHint} parameter as well as the {@code requiredCapacity}
     * parameter to determine the next size.
     *
     * @param length
     *         the current length of the array
     * @param requiredCapacity
     *         the immediately required capacity
     * @param nextCapacityHint
     *         a hint for future capacity requirements that may not be required as of now
     *
     * @return the size of an array that is guaranteed to hold {@code requiredCapacity} number of elements.
     */
    public static int computeNewCapacity(int length, int requiredCapacity, int nextCapacityHint) {
        if (requiredCapacity < length) {
            return length;
        }

        @SuppressWarnings("PMD.UselessParentheses") // parenthesis make things clearer
        int newCapacity = (length * 3) / 2 + 1;

        if (newCapacity < nextCapacityHint) {
            newCapacity = nextCapacityHint;
        }

        if (newCapacity < requiredCapacity) {
            newCapacity = requiredCapacity;
        }

        return newCapacity;
    }

    /**
     * Returns an immutable iterator that iterates over the contents of the given array.
     *
     * @param array
     *         the array over whose contents should be iterated
     * @param <E>
     *         element type
     *
     * @return an iterator for the contents of the array
     */
    public static <E> Iterator<E> iterator(E[] array) {
        return new ArrayIterator<>(array);
    }

    /**
     * Computes for the given array its prefix sum and directly stores the result in the given array.
     *
     * @param array
     *         the elements of the sum
     * @param startInclusive
     *         the start index from which to begin the computation (inclusive)
     * @param endExclusive
     *         the end index at which to stop the computation (exclusive)
     */
    public static void prefixSum(int[] array, int startInclusive, int endExclusive) {
        for (int i = startInclusive + 1; i < endExclusive; i++) {
            array[i] += array[i - 1];
        }
    }

    /**
     * Sorts the given array according to the given keys using the heapsort sorting algorithm.
     *
     * @param arr
     *         the array to sort
     * @param keys
     *         the array of keys, i.e. element i will be sorted according to {@code keys[i]}.
     */
    public static void heapsort(int[] arr, int[] keys) {

        int start = arr.length / 2;
        int end = arr.length;

        while (end > 1) {
            if (start > 0) {
                start--;
            } else {
                end--;
                swap(arr, end, 0);
            }

            int root = start;
            while (2 * root + 1 < end) {
                int child = 2 * root + 1;
                if (child + 1 < end && keys[arr[child]] < keys[arr[child + 1]]) {
                    child++;
                }

                if (keys[arr[root]] < keys[arr[child]]) {
                    swap(arr, root, child);
                    root = child;
                } else {
                    break;
                }
            }
        }
    }

    private static void swap(int[] arr, int i, int j) {
        int tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }
}
