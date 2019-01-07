package org.hotteam67.firebaseviewer.data;

import org.hotteam67.firebaseviewer.tableview.tablemodel.CellModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.ColumnHeaderModel;
import org.hotteam67.firebaseviewer.tableview.tablemodel.RowHeaderModel;

import org.hotteam67.common.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Jakob on 2/8/2018.
 */

public final class Sort {
    public static DataTable BubbleSortAscendingByRowHeader(DataTable input)
    {
        List<ColumnHeaderModel> columns = input.GetColumns();
        List<List<CellModel>> cells = input.GetCells();
        List<RowHeaderModel> rows = input.GetRowHeaders();

        boolean changed = true;
        while (changed)
        {
            changed = false;
            try {
                for (int i = 0; i < cells.size(); ++i)
                {
                    List<CellModel> row = cells.get(i);

                    int value = Integer.valueOf(rows.get(i).getData());

                    if (i + 1 >= cells.size())
                        continue;

                    int nextValue = Integer.valueOf(
                            rows.get(i + 1).getData());

                    if (value > nextValue)
                    {
                        cells.set(i, cells.get(i + 1));
                        RowHeaderModel prevRow = rows.get(i);
                        rows.set(i, rows.get(i + 1));
                        cells.set(i + 1, row);
                        rows.set(i + 1, prevRow);
                        changed = true;
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return new DataTable(columns, cells, rows);
    }

    public static DataTable SortByColumn(DataTable input, int column,
                                         boolean ascending)
    {
        if (input == null) return null;
        Constants.Time("Sort()");
        List<ColumnHeaderModel> columns = input.GetColumns();
        List<List<CellModel>> newCells = input.GetCells();
        List<RowHeaderModel> rows = input.GetRowHeaders();

        List<List<CellModel>> oldCells = new ArrayList<>(newCells);

        newCells = com.annimon.stream.Stream.of(newCells).sorted((cells1, cells2) -> Compare(cells1.get(column).getData(),
                cells2.get(column).getData())).collect(com.annimon.stream.Collectors.toList());

        if (!ascending)
            Collections.reverse(newCells);

        List<RowHeaderModel> newRows = new ArrayList<>();
        for (int i = 0; i < rows.size(); ++i)
        {
            newRows.add(null);
        }
        for (int i = 0; i < rows.size(); ++i)
        {
            int newIndex = newCells.indexOf(oldCells.get(i));
            newRows.set(newIndex, rows.get(i));
        }

        Constants.Time("Sort()");
        return new DataTable(columns, newCells, newRows);
    }

    private static int Compare(String item1, String item2)
    {
        if (item1.equals(item2))
            return 0;

        if (item1.equals("N/A"))
            return -1;
        if (item2.equals("N/A"))
            return 1;

        try
        {
            double value = Double.valueOf(item1);
            double nextValue = Double.valueOf(item2);

            if (value < nextValue)
                return -1;
            else
                return 1;
        }
        catch (NumberFormatException e)
        {
            return item1.compareTo(item2);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return 0;
        }
    }

}