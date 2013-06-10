/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.model;

import java.util.Date;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

public class Template extends Transaction {
  public String title;
  public boolean isTransfer;

  public static final Uri CONTENT_URI = TransactionProvider.TEMPLATES_URI;

  public Template(Transaction t, String title) {
    this.title = title;
    this.accountId = t.accountId;
    this.amount = t.amount;
    this.catId = t.catId;
    this.comment = t.comment;
    this.methodId = t.methodId;
    this.payee = t.payee;
    //for Transfers we store -1 as peer since it needs to be different from 0,
    //but we are not interested in which was the transfer_peer of the transfer
    //from which the template was derived;
    this.isTransfer = t.transfer_peer != null;
    this.transfer_account = t.transfer_account;
  }
  public Template(long accountId,long amount) {
    super(accountId,amount);
    title = "";
  }
  public static Template getTypedNewInstance(boolean mOperationType, long accountId) {
    Template t = new Template(accountId,0);
    t.transfer_peer = mOperationType == MyExpenses.TYPE_TRANSACTION ? null : -1L;
    return t;
  }
  public void setDate(Date date){
    //templates have no date
  }
  public static Template getInstanceFromDb(long id) {
    String[] projection = new String[] {KEY_ROWID,KEY_AMOUNT,KEY_COMMENT, KEY_CATID,
        SHORT_LABEL,KEY_PAYEE,KEY_TRANSFER_PEER,KEY_TRANSFER_ACCOUNT,KEY_ACCOUNTID,KEY_METHODID,KEY_TITLE};
    Cursor c = MyApplication.cr().query(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), projection,null,null, null);
    if (c == null || c.getCount() == 0) {
      return null;
      //TODO throw DataObjectNotFoundException
    }
    c.moveToFirst();
    Template t = new Template(c.getLong(c.getColumnIndexOrThrow(KEY_ACCOUNTID)),
        c.getLong(c.getColumnIndexOrThrow(KEY_AMOUNT))  
        );
    if (t.isTransfer = c.getInt(c.getColumnIndexOrThrow(KEY_TRANSFER_PEER)) > 0) {
      t.transfer_account = Utils.getLongOrNull(c, KEY_TRANSFER_ACCOUNT);
    } else {
      t.methodId = Utils.getLongOrNull(c, KEY_METHODID);
      t.catId = Utils.getLongOrNull(c, KEY_CATID);
      t.payee = c.getString(
          c.getColumnIndexOrThrow(KEY_PAYEE));
    }
    t.id = id;
    t.comment = c.getString(
        c.getColumnIndexOrThrow(KEY_COMMENT));
    t.label =  c.getString(c.getColumnIndexOrThrow(KEY_LABEL));
    t.title = c.getString(c.getColumnIndexOrThrow(KEY_TITLE));
    c.close();
    return t;
  }
  /**
   * Saves the new template, or updated an existing one
   * @return the Uri of the template. Upon creation it is returned from the content provider, null if inserting fails on constraints
   */
  public Uri save() {
    Uri uri;
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, comment);
    initialValues.put(KEY_AMOUNT, amount.getAmountMinor());
    initialValues.put(KEY_CATID, catId);
    initialValues.put(KEY_TRANSFER_ACCOUNT, transfer_account);
    initialValues.put(KEY_PAYEE, payee);
    initialValues.put(KEY_METHODID, methodId);
    initialValues.put(KEY_TITLE, title);
    if (id == 0) {
      initialValues.put(KEY_ACCOUNTID, accountId);
      initialValues.put(KEY_TRANSFER_PEER, isTransfer);
      try {
        uri = MyApplication.cr().insert(CONTENT_URI, initialValues);
      } catch (SQLiteConstraintException e) {
        return null;
      }
      id = ContentUris.parseId(uri);
    } else {
      org.totschnig.myexpenses.model.ContribFeature.EDIT_TEMPLATE.recordUsage();
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
      try {
        MyApplication.cr().update(uri, initialValues, null, null);
      } catch (SQLiteConstraintException e) {
        return null;
      }
    }
    return uri;
  }
  public static boolean delete(long id) {
    return MyApplication.cr().delete(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),null,null) > 0;
  }
  public static int countPerMethod(long methodId) {
    return countPerMethod(CONTENT_URI,methodId);
  }
  public static int countPerAccount(long accountId) {
    return countPerAccount(CONTENT_URI,accountId);
  }
  public static int countAll() {
    return countAll(CONTENT_URI);
  }
}