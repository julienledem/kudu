// Copyright 2014 Cloudera, Inc.
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
#include "kudu/util/threadlocal.h"

#include <pthread.h>

#include <glog/logging.h>

#include "kudu/gutil/once.h"
#include "kudu/util/errno.h"

namespace kudu {
namespace threadlocal {
namespace internal {

// One key used by the entire process to attach destructors on thread exit.
static pthread_key_t destructors_key;

// The above key must only be initialized once per process.
static GoogleOnceType once = GOOGLE_ONCE_INIT;

// Call all the destructors associated with all THREAD_LOCAL instances in this
// thread.
static void InvokeDestructors(void* t) {
  PerThreadDestructorList* d = reinterpret_cast<PerThreadDestructorList*>(t);
  while (d != NULL) {
    d->destructor(d->arg);
    PerThreadDestructorList* next = d->next;
    delete d;
    d = next;
  }
}

// This key must be initialized only once.
static void CreateKey() {
  int ret = pthread_key_create(&destructors_key, &InvokeDestructors);
  // Linux supports up to 1024 keys, we will use only one for all thread locals.
  CHECK_EQ(0, ret) << "pthread_key_create() failed, cannot add destructor to thread: "
      << "error " << ret << ": " << ErrnoToString(ret);
}

// Adds a destructor to the list.
void AddDestructor(PerThreadDestructorList* p) {
  GoogleOnceInit(&once, &CreateKey);

  // Returns NULL if nothing is set yet.
  p->next = reinterpret_cast<PerThreadDestructorList*>(pthread_getspecific(destructors_key));
  int ret = pthread_setspecific(destructors_key, p);
  // The only time this check should fail is if we are out of memory, or if
  // somehow key creation failed, which should be caught by the above CHECK.
  CHECK_EQ(0, ret) << "pthread_setspecific() failed, cannot update destructor list: "
      << "error " << ret << ": " << ErrnoToString(ret);
}

} // namespace internal
} // namespace threadlocal
} // namespace kudu
