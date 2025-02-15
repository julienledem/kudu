// Copyright 2013 Cloudera, Inc.
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

#include <gtest/gtest.h>
#include <tr1/memory>
#include <tr1/unordered_set>

#include "kudu/util/test_util.h"
#include "kudu/tablet/mock-rowsets.h"
#include "kudu/tablet/rowset.h"
#include "kudu/tablet/rowset_tree.h"
#include "kudu/tablet/compaction_policy.h"

using std::tr1::shared_ptr;
using std::tr1::unordered_set;

namespace kudu {
namespace tablet {

// Simple test for budgeted compaction: with three rowsets which
// mostly overlap, and an high budget, they should all be selected.
TEST(TestCompactionPolicy, TestBudgetedSelection) {
  RowSetVector vec;
  vec.push_back(shared_ptr<RowSet>(new MockDiskRowSet("C", "c")));
  vec.push_back(shared_ptr<RowSet>(new MockDiskRowSet("B", "a")));
  vec.push_back(shared_ptr<RowSet>(new MockDiskRowSet("A", "b")));

  RowSetTree tree;
  ASSERT_OK(tree.Reset(vec));

  const int kBudgetMb = 1000; // enough to select all
  BudgetedCompactionPolicy policy(kBudgetMb);

  std::tr1::unordered_set<RowSet*> picked;
  double quality = 0;
  ASSERT_OK(policy.PickRowSets(tree, &picked, &quality, NULL));
  ASSERT_EQ(3, picked.size());
  ASSERT_GE(quality, 1.0);
}

} // namespace tablet
} // namespace kudu
