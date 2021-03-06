/*
 * Copyright (c) 2014, Aalesund University College 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package no.hials.jiop.generic.swarm.bfo;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.hials.jiop.generic.Evaluator;
import no.hials.jiop.generic.GeneralPopulationBasedAlgorithm;
import no.hials.jiop.generic.candidates.Candidate;
import no.hials.jiop.generic.candidates.bacterium.BacteriaCandidate;
import no.hials.jiop.generic.factories.NumericCandidateFactory;

/**
 * Bacterial Foraging Optimization based on an article by James McCaffrey:
 * http://msdn.microsoft.com/en-us/magazine/hh882453.aspx
 *
 * @author Lars Ivar Hatledal
 */
public class BacterialForagingOptimization<E> extends GeneralPopulationBasedAlgorithm<E> {

    private int nc = 8; //chemotactic steps
    private int ns = 3; //maximum number of times a bacterium will swim in the same direction
    private int nre = 3; //the number of reproduction steps
    private double ped = 0.25; // probability of a particular bacterium being dispersed
    private double ci = 0.05; //basic swim length for each bacterium

    private boolean multiThreaded;

    public BacterialForagingOptimization(int size, NumericCandidateFactory<E> candidateFactory, Evaluator<E> evaluator, boolean multiThreaded) {
        super(size, candidateFactory, evaluator, multiThreaded ? "Multi-Threaded Bacterial Foraging Optimization" : "Single-Threaded Bacterial Foraging Optimization");
        this.multiThreaded = multiThreaded;
    }

    public BacterialForagingOptimization(int size, int nc, int ns, int nre, double ped, double ci, NumericCandidateFactory<E> candidateFactory, Evaluator<E> evaluator, boolean multiThreaded) {
        this(size, nc, ns, nre, ped, ci, candidateFactory, evaluator, multiThreaded ? "Multi-Threaded Bacterial Foraging Optimization" : "Single-Threaded Bacterial Foraging Optimization", multiThreaded);
    }

    public BacterialForagingOptimization(int size, int nc, int ns, int nre, double ped, double ci, NumericCandidateFactory<E> candidateFactory, Evaluator<E> evaluator, String name, boolean multiThreaded) {
        super(size, candidateFactory, evaluator, name);
        this.multiThreaded = multiThreaded;
        this.ped = ped;
        this.nre = nre;
        this.ci = ci;
        this.nc = nc;
        this.ns = ns;
    }

    @Override
    protected void singleIteration() {
        for (int k = 0; k < nre; k++) // reproduce-eliminate loop
        {
            for (int j = 0; j < nc; j++) // chemotactic loop; the lifespan of each bacterium
            {
                // reset the health of each bacterium to 0.0 
                for (int i = 0; i < size(); i++) {
                    ((BacteriaCandidate<E>) population.get(i)).setHealth(0);
                }

                for (Candidate<E> c : population) // each bacterium
                {
                    if (multiThreaded) {
                        getCompletionService().submit(() -> threadingTask1((BacteriaCandidate<E>) c), null);
                    } else {
                        threadingTask1((BacteriaCandidate<E>) c);
                    }
//                    BacteriaCandidate<E> b = (BacteriaCandidate<E>) c;
//                    double[] tumble = new double[getDimension()]; // tumble (point in a new direction)
//                    for (int p = 0; p < getDimension(); p++) {
//                        tumble[p] = 2.0 * rng.nextDouble() - 1.0;
//                    } // (hi - lo) * r + lo => random i [-1, +1]
//                    double rootProduct = 0.0;
//                    for (int p = 0; p < getDimension(); p++) {
//                        rootProduct += (tumble[p] * tumble[p]);
//                    }
//
//                    for (int p = 0; p < getDimension(); p++) {
//                        double value = b.get(p).doubleValue() + (ci * tumble[p]) / rootProduct;
//                        b.set(p, value);
//                    } // move in new direction
//
//                    // update costs of new position
//                    b.clamp(0, 1);
//                    b.setPrevCost(b.getCost());
//                    evaluate(b);
//                    b.setHealth(b.getHealth() + b.getCost()); // health is an accumulation of costs during bacterium's life
//
//                    // new best?
//                    setBestCandidateIfBetter(b);
//
//                    int m = 0; // swim or not based on prev and curr costs
//                    while (m < ns && b.getCost() < b.getPrevCost()) // we are improving
//                    {
//                        m++; // swim counter
//                        for (int p = 0; p < getDimension(); p++) {
//                            double value = b.get(p).doubleValue() + (ci * tumble[p]) / rootProduct;
//                            b.set(p, value);
//
//                        } // move in current direction
//                        b.clamp(0, 1);
//                        b.setPrevCost(b.getCost()); // update costs
//                        evaluate(b);
//                        setBestCandidateIfBetter(b);
//                    } // while improving
//
//                } // i, each bacterium in the chemotactic loop
                } // j, chemotactic loop

                if (multiThreaded) {
                    for (Candidate<E> c : population) {
                        try {
                            getCompletionService().take().get();
                        } catch (InterruptedException | ExecutionException ex) {
                            Logger.getLogger(BacterialForagingOptimization.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                // reproduce the healthiest half of bacteria, eliminate the other half
                Collections.sort(population, (Candidate<E> c1, Candidate<E> c2) -> {
                    BacteriaCandidate<E> b1 = (BacteriaCandidate<E>) c1;
                    BacteriaCandidate<E> b2 = (BacteriaCandidate<E>) c2;
                    if (b1.getHealth() == b2.getHealth()) {
                        return 0;
                    } else if (b1.getHealth() < b2.getHealth()) {
                        return -1;
                    } else {
                        return 1;
                    }
                }); // sort from smallest health (best) to highest health (worst)
                for (int left = 0; left < size() / 2; left++) // left points to a bacterium that will reproduce
                {
                    int right = left + size() / 2; // right points to a bad bacterium in the rigt side of array that will die
                    population.set(right, getPopulation().get(left).copy());
                }

            } // k, reproduction loop

            // eliminate-disperse
            for (Candidate<E> c : population) {
                if (multiThreaded) {
                    getCompletionService().submit(() -> threadingTask2((BacteriaCandidate<E>) c), null);
                } else {
                    threadingTask2((BacteriaCandidate<E>) c);
                }
//            BacteriaCandidate<E> b = (BacteriaCandidate<E>) c;
//            double prob = rng.nextDouble();
//            if (prob < ped) // disperse this bacterium to a random position
//            {
//                for (int p = 0; p < getDimension(); p++) {
//                    double x = rng.nextDouble();
//                    b.set(p, x);
//                }
//                // update costs
//                b.setHealth(0);
//                b.setPrevCost(b.getCost());
//                evaluate(b);
//
//                setBestCandidateIfBetter(b);
            }

//        }
            if (multiThreaded) {
                for (Candidate<E> c : population) {
                    try {
                        getCompletionService().take().get();
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(BacterialForagingOptimization.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    private void threadingTask1(BacteriaCandidate<E> b) {
        double[] tumble = new double[getDimension()]; // tumble (point in a new direction)
        for (int p = 0; p < getDimension(); p++) {
            tumble[p] = 2.0 * rng.nextDouble() - 1.0;
        } // (hi - lo) * r + lo => random i [-1, +1]
        double rootProduct = 0.0;
        for (int p = 0; p < getDimension(); p++) {
            rootProduct += (tumble[p] * tumble[p]);
        }

        for (int p = 0; p < getDimension(); p++) {
            double value = b.get(p).doubleValue() + (ci * tumble[p]) / rootProduct;
            b.set(p, value);
        } // move in new direction

        // update costs of new position
        b.clamp(0, 1);
        b.setPrevCost(b.getCost());
        evaluate(b);
        b.setHealth(b.getHealth() + b.getCost()); // health is an accumulation of costs during bacterium's life

        // new best?
        setBestCandidateIfBetter(b);

        int m = 0; // swim or not based on prev and curr costs
        while (m < ns && b.getCost() < b.getPrevCost()) // we are improving
        {
            m++; // swim counter
            for (int p = 0; p < getDimension(); p++) {
                double value = b.get(p).doubleValue() + (ci * tumble[p]) / rootProduct;
                b.set(p, value);

            } // move in current direction
            b.clamp(0, 1);
            b.setPrevCost(b.getCost()); // update costs
            evaluate(b);
            setBestCandidateIfBetter(b);
        } // while improving
    }

    private void threadingTask2(BacteriaCandidate<E> b) {
        double prob = rng.nextDouble();
        if (prob < ped) // disperse this bacterium to a random position
        {
            for (int p = 0; p < getDimension(); p++) {
                double x = rng.nextDouble();
                b.set(p, x);
            }
            // update costs
            b.setHealth(0);
            b.setPrevCost(b.getCost());
            evaluate(b);

            setBestCandidateIfBetter(b);
        }

    }

    public int getNc() {
        return nc;
    }

    public void setNc(int nc) {
        this.nc = nc;
    }

    public int getNs() {
        return ns;
    }

    public void setNs(int ns) {
        this.ns = ns;
    }

    public int getNre() {
        return nre;
    }

    public void setNre(int nre) {
        this.nre = nre;
    }

    public double getPed() {
        return ped;
    }

    public void setPed(double ped) {
        this.ped = ped;
    }

    public double getCi() {
        return ci;
    }

    public void setCi(double ci) {
        this.ci = ci;
    }

//    @Override
//    public int getNumberOfFreeParameters() {
//        return 6;
//    }
//
//    @Override
//    public void setFreeParameters(DoubleArray array) {
//        this.size = (int) new NormUtil(1, 0, 1000, 10).normalize(array.get(0));
//        this.nc = (int) new NormUtil(1, 0, 10, 2).normalize(array.get(1));
//        this.nre = (int) new NormUtil(1, 0, 10, 1).normalize(array.get(2));
//        this.ns = (int) new NormUtil(1, 0, 10, 1).normalize(array.get(3));
//        this.ped = new NormUtil(1, 0, 1, 0.1).normalize(array.get(4));
//        this.ci =  new NormUtil(1, 0, 0.2, 0.005).normalize(array.get(5));
//    }
//    @Override
//    public DoubleArray getFreeParameters() {
//        return new DoubleArray(size, nc, nre, ns, ped, ci);
//    }
}
