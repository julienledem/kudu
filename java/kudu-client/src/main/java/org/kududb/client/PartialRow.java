// Copyright 2015 Cloudera, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.kududb.client;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import com.google.common.collect.Lists;
import org.kududb.ColumnSchema;
import org.kududb.Schema;
import org.kududb.Type;
import org.kududb.annotations.InterfaceAudience;
import org.kududb.annotations.InterfaceStability;

/**
 * Class used to represent parts of a row along with its schema.
 *
 * Values can be replaced as often as needed, but once the enclosing {@link Operation} is applied
 * then they cannot be changed again. This means that a PartialRow cannot be reused.
 *
 * Each PartialRow is backed by an byte array where all the cells (except strings and binary data)
 * are written. The others are kept in a List.
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class PartialRow {

  private final Schema schema;
  // Variable length data. If string, will be UTF-8 encoded.
  private final List<byte[]> varLengthData;
  private final byte[] rowAlloc;
  private final BitSet columnsBitSet;
  private final BitSet nullsBitSet;
  private boolean frozen = false;

  /**
   * This is not a stable API, prefer using {@link Schema#newPartialRow()}
   * to create a new partial row.
   * @param schema the schema to use for this row
   */
  public PartialRow(Schema schema) {
    this.schema = schema;
    this.columnsBitSet = new BitSet(this.schema.getColumnCount());
    this.nullsBitSet = schema.hasNullableColumns() ?
        new BitSet(this.schema.getColumnCount()) : null;
    this.rowAlloc = new byte[schema.getRowSize()];
    // Pre-fill the array with nulls. We'll only replace cells that have varlen values.
    this.varLengthData = Arrays.asList(new byte[this.schema.getColumnCount()][]);
  }

  /**
   * Creates a new partial row by deep-copying the data-fields of the provided partial row.
   * @param row the partial row to copy
   */
  PartialRow(PartialRow row) {
    this.schema = row.schema;

    this.varLengthData = Lists.newArrayListWithCapacity(row.varLengthData.size());
    for (byte[] data: row.varLengthData) {
      this.varLengthData.add(data == null ? null : data.clone());
    }

    this.rowAlloc = row.rowAlloc.clone();
    this.columnsBitSet = (BitSet) row.columnsBitSet.clone();
    this.nullsBitSet = row.nullsBitSet == null ? null : (BitSet) row.nullsBitSet.clone();
  }

  /**
   * Add a boolean for the specified column.
   * @param columnIndex the column's index in the schema
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addBoolean(int columnIndex, boolean val) {
    checkColumn(schema.getColumnByIndex(columnIndex), Type.BOOL);
    rowAlloc[getPositionInRowAllocAndSetBitSet(columnIndex)] = (byte) (val ? 1 : 0);
  }

  /**
   * Add a boolean for the specified column.
   * @param columnName Name of the column
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addBoolean(String columnName, boolean val) {
    addBoolean(schema.getColumnIndex(columnName), val);
  }

  /**
   * Add a byte for the specified column.
   * @param columnIndex the column's index in the schema
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addByte(int columnIndex, byte val) {
    checkColumn(schema.getColumnByIndex(columnIndex), Type.INT8);
    rowAlloc[getPositionInRowAllocAndSetBitSet(columnIndex)] = val;
  }

  /**
   * Add a byte for the specified column.
   * @param columnName Name of the column
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addByte(String columnName, byte val) {
    addByte(schema.getColumnIndex(columnName), val);
  }

  /**
   * Add a short for the specified column.
   * @param columnIndex the column's index in the schema
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addShort(int columnIndex, short val) {
    checkColumn(schema.getColumnByIndex(columnIndex), Type.INT16);
    Bytes.setShort(rowAlloc, val, getPositionInRowAllocAndSetBitSet(columnIndex));
  }

  /**
   * Add a short for the specified column.
   * @param columnName Name of the column
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addShort(String columnName, short val) {
    addShort(schema.getColumnIndex(columnName), val);
  }

  /**
   * Add an int for the specified column.
   * @param columnIndex the column's index in the schema
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addInt(int columnIndex, int val) {
    checkColumn(schema.getColumnByIndex(columnIndex), Type.INT32);
    Bytes.setInt(rowAlloc, val, getPositionInRowAllocAndSetBitSet(columnIndex));
  }

  /**
   * Add an int for the specified column.
   * @param columnName Name of the column
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addInt(String columnName, int val) {
    addInt(schema.getColumnIndex(columnName), val);
  }

  /**
   * Add an long for the specified column.
   * @param columnIndex the column's index in the schema
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addLong(int columnIndex, long val) {
    checkColumn(schema.getColumnByIndex(columnIndex), Type.INT64, Type.TIMESTAMP);
    Bytes.setLong(rowAlloc, val, getPositionInRowAllocAndSetBitSet(columnIndex));
  }

  /**
   * Add an long for the specified column.
   *
   * If this is a TIMESTAMP column, the long value provided should be the number of microseconds
   * between a given time and January 1, 1970 UTC.
   * For example, to encode the current time, use setLong(System.currentTimeMillis() * 1000);
   *
   * @param columnName Name of the column
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addLong(String columnName, long val) {
    addLong(schema.getColumnIndex(columnName), val);
  }

  /**
   * Add an float for the specified column.
   * @param columnIndex the column's index in the schema
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addFloat(int columnIndex, float val) {
    checkColumn(schema.getColumnByIndex(columnIndex), Type.FLOAT);
    Bytes.setFloat(rowAlloc, val, getPositionInRowAllocAndSetBitSet(columnIndex));
  }

  /**
   * Add an float for the specified column.
   * @param columnName Name of the column
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addFloat(String columnName, float val) {
    addFloat(schema.getColumnIndex(columnName), val);
  }

  /**
   * Add an double for the specified column.
   * @param columnIndex the column's index in the schema
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addDouble(int columnIndex, double val) {
    checkColumn(schema.getColumnByIndex(columnIndex), Type.DOUBLE);
    Bytes.setDouble(rowAlloc, val, getPositionInRowAllocAndSetBitSet(columnIndex));
  }

  /**
   * Add an double for the specified column.
   * @param columnName Name of the column
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addDouble(String columnName, double val) {
    addDouble(schema.getColumnIndex(columnName), val);
  }

  /**
   * Add a String for the specified column.
   * @param columnIndex the column's index in the schema
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addString(int columnIndex, String val) {
    addStringUtf8(columnIndex, Bytes.fromString(val));
  }

  /**
   * Add a String for the specified column.
   * @param columnName Name of the column
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addString(String columnName, String val) {
    addStringUtf8(columnName, Bytes.fromString(val));
  }

  /**
   * Add a String for the specified value, encoded as UTF8.
   * Note that the provided value must not be mutated after this.
   * @param columnIndex the column's index in the schema
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addStringUtf8(int columnIndex, byte[] val) {
    // TODO: use Utf8.isWellFormed from Guava 16 to verify that
    // the user isn't putting in any garbage data.
    checkColumn(schema.getColumnByIndex(columnIndex), Type.STRING);
    addVarLengthData(columnIndex, val);
  }

  /**
   * Add a String for the specified value, encoded as UTF8.
   * Note that the provided value must not be mutated after this.
   * @param columnName Name of the column
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   *
   */
  public void addStringUtf8(String columnName, byte[] val) {
    addStringUtf8(schema.getColumnIndex(columnName), val);
  }

  /**
   * Add binary data with the specified value.
   * Note that the provided value must not be mutated after this.
   * @param columnIndex the column's index in the schema
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addBinary(int columnIndex, byte[] val) {
    checkColumn(schema.getColumnByIndex(columnIndex), Type.BINARY);
    addVarLengthData(columnIndex, val);
  }

  /**
   * Add binary data with the specified value.
   * Note that the provided value must not be mutated after this.
   * @param columnName Name of the column
   * @param val value to add
   * @throws IllegalArgumentException if the column doesn't exist or if the value doesn't match
   * the column's type
   * @throws IllegalStateException if the row was already applied
   */
  public void addBinary(String columnName, byte[] val) {
    addBinary(schema.getColumnIndex(columnName), val);
  }

  private void addVarLengthData(int columnIndex, byte[] val) {
    varLengthData.set(columnIndex, val);
    // Set the usage bit but we don't care where it is.
    getPositionInRowAllocAndSetBitSet(columnIndex);
    // We don't set anything in row alloc, it will be managed at encoding time.
  }

  /**
   * Set the specified column to null
   * @param columnIndex the column's index in the schema
   * @throws IllegalArgumentException if the column doesn't exist or cannot be set to null
   * @throws IllegalStateException if the row was already applied
   */
  public void setNull(int columnIndex) {
    setNull(this.schema.getColumnByIndex(columnIndex));
  }

  /**
   * Set the specified column to null
   * @param columnName Name of the column
   * @throws IllegalArgumentException if the column doesn't exist or cannot be set to null
   * @throws IllegalStateException if the row was already applied
   */
  public void setNull(String columnName) {
    setNull(this.schema.getColumn(columnName));
  }

  private void setNull(ColumnSchema column) {
    assert nullsBitSet != null;
    checkNotFrozen();
    checkColumnExists(column);
    if (!column.isNullable()) {
      throw new IllegalArgumentException(column.getName() + " cannot be set to null");
    }
    int idx = schema.getColumns().indexOf(column);
    columnsBitSet.set(idx);
    nullsBitSet.set(idx);
  }

  /**
   * Verifies if the column exists and belongs to one of the specified types
   * It also does some internal accounting
   * @param column column the user wants to set
   * @param types types we expect
   * @throws IllegalArgumentException if the column or type was invalid
   * @throws IllegalStateException if the row was already applied
   */
  private void checkColumn(ColumnSchema column, Type... types) {
    checkNotFrozen();
    checkColumnExists(column);
    for(Type type : types) {
      if (column.getType().equals(type)) return;
    }
    throw new IllegalArgumentException(String.format("%s isn't %s, it's %s", column.getName(),
        Arrays.toString(types), column.getType().getName()));
  }

  /**
   * @param column column the user wants to set
   * @throws IllegalArgumentException if the column doesn't exist
   */
  private void checkColumnExists(ColumnSchema column) {
    if (column == null)
      throw new IllegalArgumentException("Column name isn't present in the table's schema");
  }

  /**
   * @throws IllegalStateException if the row was already applied
   */
  private void checkNotFrozen() {
    if (frozen) {
      throw new IllegalStateException("This row was already applied and cannot be modified.");
    }
  }

  /**
   * Sets the column bit set for the column index, and returns the column's offset.
   * @param columnIndex the index of the column to get the position for and mark as set
   * @return the offset in rowAlloc for the column
   */
  private int getPositionInRowAllocAndSetBitSet(int columnIndex) {
    columnsBitSet.set(columnIndex);
    return schema.getColumnOffset(columnIndex);
  }

  /**
   * Tells if the specified column was set by the user
   * @param column column's index in the schema
   * @return true if it was set, else false
   */
  boolean isSet(int column) {
    return this.columnsBitSet.get(column);
  }

  /**
   * Tells if the specified column was set to null by the user
   * @param column column's index in the schema
   * @return true if it was set, else false
   */
  boolean isSetToNull(int column) {
    if (this.nullsBitSet == null) {
      return false;
    }
    return this.nullsBitSet.get(column);
  }

  /**
   * Returns the encoded primary key of the row.
   * @return a byte array containing an encoded primary key
   */
  public byte[] encodePrimaryKey() {
    return new KeyEncoder().encodePrimaryKey(this);
  }

  /**
   * Get the schema used for this row.
   * @return a schema that came from KuduTable
   */
  Schema getSchema() {
    return schema;
  }

  /**
   * Get the list variable length data cells that were added to this row.
   * @return a list of binary data, may be empty
   */
  List<byte[]> getVarLengthData() {
    return varLengthData;
  }

  /**
   * Get the byte array that contains all the data added to this partial row. Variable length data
   * is contained separately, see {@link #getVarLengthData()}. In their place you'll find their
   * index in that list and their size.
   * @return a byte array containing the data for this row, except strings
   */
  byte[] getRowAlloc() {
    return rowAlloc;
  }

  /**
   * Get the bit set that indicates which columns were set.
   * @return a bit set for columns with data
   */
  BitSet getColumnsBitSet() {
    return columnsBitSet;
  }

  /**
   * Get the bit set for the columns that were specifically set to null
   * @return a bit set for null columns
   */
  BitSet getNullsBitSet() {
    return nullsBitSet;
  }

  /**
   * Prevents this PartialRow from being modified again. Can be called multiple times.
   */
  void freeze() {
    this.frozen = true;
  }
}
