__author__ = 'ugonzjo'
import pandas as pd
import random
import os
'''
This scripts samples observations from a CSV so that knowledge components appear in training and testing. It relies on pandas.
To install pandas easily, use pip:

pip install pandas
'''


def split( df, train_pct= 0.6, student_column="user_id",):

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


def main(filename="../../../datasets/sweet.csv", min_students=2, min_observations=100, sep=",", train=0.8, seed=0):
    random.seed(seed)
    df = pd.read_csv(filename, sep=sep)
    kcs = df["KCs"].unique()

    trains = []
    tests =  []
    for kc in kcs:
        df_kc = df[ df["KCs"] == kc]
        df_train,  df_test, train_students, test_students = split(df_kc, train_pct=train, student_column="student")

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


    train_filename = "{}/filtered_{}0_train.csv".format(path, name)
    test_filename  = "{}/filtered_{}0_test.csv".format(path, name)

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
