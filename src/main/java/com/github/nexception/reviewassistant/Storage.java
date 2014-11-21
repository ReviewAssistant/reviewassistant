/*
The MIT License (MIT)

Copyright (c) 2014 Gustav Jansson Ekstrand (gustav.jp@live.se), Simon Wessel (nllptr), William Phan (william.da.phan@gmail.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/



package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;

/**
 * The Storage interface is used to store and fetch calculations.
 */
public interface Storage {

    /**
     * Stores the provided calculation.
     * @param calculation the calculation object to be stored
     */
    public void storeCalculation(Calculation calculation);

    /**
     * Returns the calculation object with the matching commit id.
     * @param commitId the commit id to look fetch
     * @return a Calculation object if one is found, null otherwise
     */
    public Calculation fetchCalculation(String commitId);
}