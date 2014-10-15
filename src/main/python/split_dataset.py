__author__ = 'ugonzjo'
import pandas as pd
import random
import os

pd.set_option('display.width', 1000)
pd.set_option('display.max_rows', 3000)

'''
This scripts samples observations from a CSV so that knowledge components appear in training and testing. It relies on pandas.
To install pandas easily, use pip:

pip install pandas
'''


def split( df,  student_column, train_pct= 0.6,):

    if train_pct  > 1:
        raise RuntimeError("Training and development set have to be (strictly) less than 100% of the students")

    students = df[student_column].unique()
    random.shuffle(students)

    train_number = int(train_pct * len(students))


    train_students = students[0:train_number]
    test_students = students[train_number: ]


    df_train =  df[student_column].isin(train_students)
    df_test   =  df[student_column].isin(test_students)


    return df_train, df_test,len(train_students), len(test_students)



def lag(df, student_column, amount):
    column_order = df.columns

    fixed_columns = [ "fold", "outcome", "problem", "step", "KCs", student_column]
    feature_columns = []
    for c in df.columns:
        if c not in fixed_columns:
            feature_columns.append(c)

    print "Feature columns: ", feature_columns


    df_lagged_features =  df.groupby(student_column)[feature_columns].shift(amount)
    l = len(df_lagged_features)

    df_new = df[fixed_columns].join(df_lagged_features)

    assert( l == len(df))
    assert (l == len(df_lagged_features))
    assert (l == len(df_new))

    df_new = df_new.fillna(0)
    return df_new[column_order]



def main(filename="../../../datasets/sweet.csv", student_column="student", lag_features=1, min_students=2, min_observations=100, sep=",", train=0.8, seed=0):
    random.seed(seed)
    df = pd.read_csv(filename, sep=sep)


    # Lag features:
    df = lag(df, student_column, 1)

    kcs = df["KCs"].unique()

    trains = []
    tests =  []
    for kc in kcs:
        df_kc = df[ df["KCs"] == kc]
        df_train,  df_test, train_students, test_students = split(df_kc, train_pct=train, student_column=student_column)

        if len(df_kc[df_train]) > min_observations and train_students > min_students:
            trains.append(df_kc[df_train])
            tests.append(df_kc[df_test])

            print kc, len(df_kc), len(df_kc[df_train]), len(df_kc[df_test]), train_students, test_students
            assert len(df_kc) == len(df_kc[df_train]) + len(df_kc[df_test])

    df_trains = pd.concat(trains, axis=0)
    df_tests = pd.concat(tests, axis=0)

    path = os.path.dirname(filename)
    name_ext  = os.path.basename(filename)
    name, ext = os.path.splitext(name_ext)


    train_filename = "{}/filtered_{}_train0.csv".format(path, name)
    test_filename  = "{}/filtered_{}_test0.csv".format(path, name)

    with open(train_filename, "w") as train:
        df_trains.to_csv(train, index=False)
    with open(test_filename, "w") as test:
        df_tests.to_csv(test, index=False)

if __name__ == "__main__":
    import sys

    args = sys.argv
    print args
    cl = {}
    for i in range(1, len(args)):  # index 0 is the filename
        pair = args[i].split('=')
        if pair[1].isdigit():
            cl[pair[0]] = int(pair[1])
        elif pair[1].lower() in ("true", "false"):
            cl[pair[0]] = (pair[1].lower() == 'true')
        else:
            cl[pair[0]] = pair[1]

    main(**cl)
