goog.provide("solid.alpha.impl.array");

// TODO: figure out how to do imports in goog modules
//window.solidInitArray = function init({ onCleanup, createRoot, untrack, createSignal, Owner, Accessor, Setter }) {
solid.alpha.impl.array.solidInitArray = function init({ onCleanup, createRoot, untrack, createSignal, Owner, Accessor, Setter }) {

const FALLBACK = Symbol("fallback");
function dispose(d) {
  for (let i = 0; i < d.length; i++) d[i]();
}
function mapArray(list, mapFn, options = {}) {
  let items = [],
      mapped = [],
      disposers = [],
      len = 0,
      setters = [],
      indexes = mapFn.length > 1 ? [] : null;
  onCleanup(() => dispose(disposers));
  return () => {
    let newItems = list() || [],
        i,
        j;
    return untrack(() => {
      let newLen = newItems.length,
          newIndices,
          newIndicesNext,
          temp,
          tempdisposers,
          tempSetters,
          tempIndexes,
          start,
          end,
          newEnd,
          item;

      // fast path for empty arrays
      if (newLen === 0) {
        if (len !== 0) {
          dispose(disposers);
          disposers = [];
          items = [];
          mapped = [];
          len = 0;
          setters = [];
          indexes && (indexes = []);
        }
        if (options.fallback) {
          items = [FALLBACK];
          mapped[0] = createRoot(disposer => {
            disposers[0] = disposer;
            return options.fallback();
          });
          len = 1;
        }
      }

      // fast path for new create
      else if (len === 0) {
        mapped = new Array(newLen);
        setters = new Array(newLen);
        for (j = 0; j < newLen; j++) {
          items[j] = newItems[j];
          mapped[j] = createRoot(mapper);
        }
        len = newLen;
      } else {
        temp = new Array(newLen);
        tempdisposers = new Array(newLen);
        tempSetters = new Array(newLen);
        indexes && (tempIndexes = new Array(newLen));


        // skip common prefix
        for (start = 0, end = Math.min(len, newLen); start < end && items[start] === newItems[start]; start++);

        // common suffix
        for (end = len - 1, newEnd = newLen - 1; end >= start && newEnd >= start && items[end] === newItems[newEnd]; end--, newEnd--) {
          temp[newEnd] = mapped[end];
          tempdisposers[newEnd] = disposers[end];
          tempSetters[newEnd] = setters[end];
          indexes && (tempIndexes[newEnd] = indexes[end]);
        }

        // 0) prepare a map of all indices in newItems, scanning backwards so we encounter them in natural order
        newIndices = new Map();
        newIndicesNext = new Array(newEnd + 1);
        for (j = newEnd; j >= start; j--) {
          item = newItems[j];
          i = newIndices.get(item);
          newIndicesNext[j] = i === undefined ? -1 : i;
          newIndices.set(item, j);
        }

        const reuseNodes = !(start === 0 && end === newLen - 1);

        // 1) step through all old items and see if they can be found in the new set; if so, save them in a temp array and mark them moved; if not, exit them
        for (i = start; i <= end; i++) {
          item = items[i];
          j = newIndices.get(item);
          if (j !== undefined && j !== -1) {
            temp[j] = mapped[i];
            tempdisposers[j] = disposers[i];
            tempSetters[j] = setters[i];
            indexes && (tempIndexes[j] = indexes[i]);
            j = newIndicesNext[j];
            newIndices.set(item, j);
          } else if (reuseNodes && i < len) {
          } else disposers[i]();
        }

        // 2) set all the new values, pulling from the temp array if copied, otherwise entering the new value
        for (j = start; j < newLen; j++) {
          if (j in temp) {
            mapped[j] = temp[j];
            disposers[j] = tempdisposers[j];
            setters[j] = tempSetters[j];
            if (indexes) {
              indexes[j] = tempIndexes[j];
              indexes[j](j);
            }
          } else if (reuseNodes && j < len) {
            setters[j](newItems[j]);
          } else mapped[j] = createRoot(mapper);
        }

        // 3) in case the new set is shorter than the old, set the length of the mapped array
        mapped = mapped.slice(0, len = newLen);

        // 4) save a copy of the mapped items for the next update
        items = newItems.slice(0);
      }
      return mapped;
    });
    function mapper(disposer) {
      const [value, setValue] = createSignal(newItems[j]);
      disposers[j] = disposer;
      setters[j] = setValue;
      if (indexes) {
        const [s, set] = createSignal(j);
        indexes[j] = set;
        return mapFn(value, s);
      }
      return mapFn(value);
    }
  };
}

return { mapArray };
}
