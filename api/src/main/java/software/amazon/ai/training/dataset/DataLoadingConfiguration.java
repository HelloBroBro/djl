/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package software.amazon.ai.training.dataset;

import java.util.List;

/**
 * DataLoadingConfiguration is used to build data loading configuration. It allows users to
 * customize loading order, automatic batching and optimize performance with multi-thread and memory
 * pining.
 */
public final class DataLoadingConfiguration {
    private long batchSize;
    private boolean shuffle;
    private Sampler<Long> sampler;
    private Sampler<List<Long>> batchSampler;
    private int numWorkers;
    private Batchifier batchifier;
    private boolean pinMemory;
    private boolean dropLast;

    private DataLoadingConfiguration(Builder builder) {
        this.batchSize = builder.batchSize;
        this.shuffle = builder.shuffle;
        this.sampler = builder.sampler;
        this.batchSampler = builder.batchSampler;
        this.numWorkers = builder.numWorkers;
        this.batchifier = builder.batchifier;
        this.pinMemory = builder.pinMemory;
        this.dropLast = builder.dropLast;
    }

    public long getBatchSize() {
        return batchSize;
    }

    public boolean getShuffle() {
        return shuffle;
    }

    public Sampler<Long> getSampler() {
        return sampler;
    }

    public Sampler<List<Long>> getBatchSampler() {
        return batchSampler;
    }

    public int getNumWorkers() {
        return numWorkers;
    }

    public Batchifier getBatchifier() {
        return batchifier;
    }

    public boolean getPinMemory() {
        return pinMemory;
    }

    public boolean getDropLast() {
        return dropLast;
    }

    public static final class Builder {
        private long batchSize;
        private boolean shuffle;
        private Sampler<Long> sampler;
        private Sampler<List<Long>> batchSampler;
        private int numWorkers;
        private Batchifier batchifier;
        private boolean pinMemory;
        private boolean dropLast;

        public Builder() {
            this.batchSize = 1;
            this.shuffle = false;
            this.numWorkers = 0;
            this.pinMemory = false;
            this.dropLast = false;
        }

        public Builder setBatchSize(long batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder setShuffle(boolean shuffle) {
            this.shuffle = shuffle;
            return this;
        }

        public Builder setSampler(Sampler<Long> sampler) {
            this.sampler = sampler;
            return this;
        }

        public Builder setBatchSampler(Sampler<List<Long>> batchSampler) {
            this.batchSampler = batchSampler;
            return this;
        }

        public Builder setNumWorkers(int workers) {
            this.numWorkers = workers;
            return this;
        }

        public Builder setBatchifier(Batchifier batchifier) {
            this.batchifier = batchifier;
            return this;
        }

        public Builder setPinMemory(boolean pinMemory) {
            this.pinMemory = pinMemory;
            return this;
        }

        public Builder setDropLast(boolean dropLast) {
            this.dropLast = dropLast;
            return this;
        }

        public DataLoadingConfiguration build() {
            // sampler is exclusive with shuffle ()
            return new DataLoadingConfiguration(this);
        }
    }
}
