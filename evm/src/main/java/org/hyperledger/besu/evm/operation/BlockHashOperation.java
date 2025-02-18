/*
 * Copyright contributors to Hyperledger Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.evm.operation;

import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;

import java.util.function.BiFunction;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;

/** The Block hash operation. */
public class BlockHashOperation extends AbstractOperation {

  /** Frontier maximum relative block delta */
  public static final int MAX_RELATIVE_BLOCK = 256;

  /**
   * Function that gets the block hash, passed in as part of TxValues.
   *
   * <p>First arg is the current block number, second is the block argument from the stack. The
   * Result is the Hash, which may be zero based on lookup rules.
   */
  public interface BlockHashLookup extends BiFunction<MessageFrame, Long, Hash> {}

  private static final int MAX_BLOCK_ARG_SIZE = 8;

  /**
   * Instantiates a new Block hash operation.
   *
   * @param gasCalculator the gas calculator
   */
  public BlockHashOperation(final GasCalculator gasCalculator) {
    super(0x40, "BLOCKHASH", 1, 1, gasCalculator);
  }

  @Override
  public OperationResult execute(MessageFrame frame, EVM evm) {
    final Bytes blockArg = frame.popStackItem().trimLeadingZeros();

    // Short-circuit if value exceeds long
    if (blockArg.size() > MAX_BLOCK_ARG_SIZE) {
      frame.pushStackItem(UInt256.ZERO);
      return new OperationResult(gasCalculator().getBlockHashOperationGasCost(null), null);
    }

    final long soughtBlock = blockArg.toLong();
    final BlockHashLookup blockHashLookup = frame.getBlockHashLookup();
    final Hash blockHash = blockHashLookup.apply(frame, soughtBlock);
    frame.pushStackItem(blockHash);
    return new OperationResult(
        gasCalculator().getBlockHashOperationGasCost(Hash.ZERO.equals(blockHash) ? null : frame),
        null);
  }
}
