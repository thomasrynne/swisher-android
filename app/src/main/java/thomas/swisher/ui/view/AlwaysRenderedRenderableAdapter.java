package thomas.swisher.ui.view;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import trikita.anvil.Anvil;
import trikita.anvil.RenderableView;

/**
 * user RenderableView for the recycled list item views so that they
 * get mounted and unmounted. This means they will be checked on every
 * Anvil.render() call
 */
public abstract class AlwaysRenderedRenderableAdapter extends BaseAdapter {

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            return new RenderableView(parent.getContext()) {
                { this.setTag(position); }
                @Override
                public void view() {
                    int currentPosition = ((Integer)this.getTag()).intValue();
                    AlwaysRenderedRenderableAdapter.this.view(currentPosition);
                }
            };
        } else {
            v.setTag(position);
            Anvil.render();
            return v;
        }
    }

    @Override
    public long getItemId(int pos) {
        return pos; // just a most common implementation
    }

    public abstract void view(int index);
}
