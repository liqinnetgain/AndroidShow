package ekoolab.com.show.views.nestlistview;

import java.util.List;

/**
 * @author zhangxutong
 * @modify Neil
 * 介绍：完全伸展开的ListView的适配器
 * 作者：zhangxutong
 * 邮箱：mcxtzhang@163.com
 * CSDN：http://blog.csdn.net/zxt0601
 * 时间： 16/09/09.
 */

public abstract class NestFullListViewAdapter<T> {
    /**
     * 条目ID
     */
    private int mItemLayoutId;
    /**
     * 数据源
     */
    private List<T> mDatas;

    public NestFullListViewAdapter(int mItemLayoutId, List<T> mDatas) {
        this.mItemLayoutId = mItemLayoutId;
        this.mDatas = mDatas;
    }

    /**
     * 被FullListView调用
     *
     * @param i
     * @param holder
     */
    public void onBind(int i, NestFullViewHolder holder) {
        //回调bind方法，多传一个data过去
        onBind(i, mDatas.get(i), holder);
    }

    /**
     * 数据绑定方法
     *
     * @param position 位置
     * @param item     数据
     * @param holder   ItemView的ViewHolder
     */
    public abstract void onBind(int position, T item, NestFullViewHolder holder);

    public int getItemLayoutId() {
        return mItemLayoutId;
    }

    public void setItemLayoutId(int mItemLayoutId) {
        this.mItemLayoutId = mItemLayoutId;
    }

    public List<T> getDatas() {
        return mDatas;
    }

    public void setDatas(List<T> mDatas) {
        this.mDatas = mDatas;
    }

}
