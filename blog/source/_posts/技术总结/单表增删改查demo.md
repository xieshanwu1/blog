---
title: 单表增删改查html
date: 2024-03-11 18:17:22
lang: zh-cn
tags: 
---

# 单表增删改查demo

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>表单增删改查</title>
    <link rel="stylesheet" href="https://cdn.bootcss.com/bootstrap/3.3.7/css/bootstrap.min.css">
    <script src="https://cdn.bootcss.com/jquery/3.2.1/jquery.min.js"></script>
    <script src="https://cdn.bootcss.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
</head>
<body>
<div class="container">
    <h1>表单增删改查</h1>
    <div class="row">
        <div class="col-md-12">
            <label for="token">token：</label>
            <input type="text" class="form-control" id="token" placeholder="请输入token" value="e6b450c578704fd685c6d46903c17445">
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <form class="form-inline">
                <div class="form-group">
                    <label for="name">姓名：</label>
                    <input type="text" class="form-control" id="name" placeholder="请输入姓名">
                </div>
                <div class="form-group">
                    <label for="age">年龄：</label>
                    <input type="text" class="form-control" id="age" placeholder="请输入年龄">
                </div>
                <button type="button" class="btn btn-primary" id="searchBtn">查询</button>
                <button type="button" class="btn btn-success" id="addBtn">新增</button>
            </form>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <table class="table table-striped">
                <thead>
                <tr>
                    <th>编号</th>
                    <th>姓名</th>
                    <th>年龄</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody id="dataBody">
                </tbody>
            </table>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <nav aria-label="Page navigation">
                <ul class="pagination">
                    <!-- pagination links will be generated dynamically -->
                </ul>
            </nav>
        </div>
    </div>

    <div class="modal fade" id="myModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title" id="myModalLabel"></h4>
                </div>
                <div class="modal-body">
                    <form class="form-horizontal">
                        <div class="form-group">
                            <label for="id" class="col-sm-2 control-label">编号</label>
                            <div class="col-sm-10">
                                <input type="text" class="form-control" id="id" placeholder="请输入编号">
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="name2" class="col-sm-2 control-label">姓名</label>
                            <div class="col-sm-10">
                                <input type="text" class="form-control" id="name2" placeholder="请输入姓名">
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="age2" class="col-sm-2 control-label">年龄</label>
                            <div class="col-sm-10">
                                <input type="text" class="form-control" id="age2" placeholder="请输入年龄">
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">关闭</button>
                    <button type="button" class="btn btn-primary" id="saveBtn">保存</button>
                </div>
            </div>
        </div>
    </div>
</div>

</body>

<script>

    var pageSize = 1; // number of records per page
    var currentPage = 1; // current page number
    var totalPage = 0; // total number of pages
    var totalRecord = 0; // total number of records
    var baseURL = window.location.origin + "/live-manager"     //此处填写自己的接口地址

    $(function () {
        // 查询按钮点击事件
        $('#searchBtn').click(function () {
            var name = $('#name').val();
            var age = $('#age').val();
            $.ajax({
                url: baseURL + '/rpa/page',
                type: 'GET',
                data: {
                    name: name,
                    age: age,
                    size: pageSize,
                    page: currentPage
                },
                beforeSend: function(xhr) {
                    if ($('#token').val()) {
                        xhr.setRequestHeader('token', $('#token').val());
                    }
                },
                headers:{
                    "Content-Type": "application/json"
                },
                success: function (data) {
                    if (data.code === 200) {
                        var html = '';
                        for (var i = 0; i < data.data.list.length; i++) {
                            var item = data.data.list[i];
                            html += '<tr>';
                            html += '<td>' + item.id + '</td>';
                            html += '<td>' + item.md5 + '</td>';
                            html += '<td>' + item.url + '</td>';
                            html += '<td>';
                            html += '<button type="button" class="btn btn-primary btn-xs" onclick="edit(' + item.id + ')">编辑</button>';
                            html += '<button type="button" class="btn btn-danger btn-xs" onclick="del(' + item.id + ')">删除</button>';
                            html += '</td>';
                            html += '</tr>';

                        }
                        $('#dataBody').html(html);
                        totalRecord = data.data.total;
                        totalPage = Math.ceil(totalRecord / pageSize);
                        updatePagination();
                    } else {
                        alert(data.msg);
                    }
                },
                error: function () {
                    alert('网络错误，请稍后重试！');
                }
            });
        });

        // 新增按钮点击事件
        $('#addBtn').click(function () {
            $('#myModal').modal('show');
            $('#myModalLabel').text('新增');
            $('#id').val('');
            $('#name2').val('');
            $('#age2').val('');
        });

        // 保存按钮点击事件
        $('#saveBtn').click(function () {
            var id = $('#id').val();
            var name = $('#name2').val();
            var age = $('#age2').val();
            var url = '';
            var type = '';
            if (id === '') {
                url = '/api/add';
                type = 'POST';
            } else {
                url = '/api/update';
                type = 'PUT';
            }
            $.ajax({
                url: url,
                type: type,
                data: {
                    id: id,
                    name: name,
                    age: age
                },
                success: function (data) {
                    if (data.code === 0) {
                        $('#myModal').modal('hide');
                        $('#searchBtn').click();
                    } else {
                        alert(data.msg);
                    }
                },
                error: function () {
                    alert('网络错误，请稍后重试！');
                }
            });
        });
    });


    // 编辑按钮点击事件
    function edit(id) {
        $.ajax({
            url: '/api/get',
            type: 'GET',
            data: {
                id: id
            },
            success: function (data) {
                if (data.code === 0) {
                    $('#myModal').modal('show');
                    $('#myModalLabel').text('编辑');
                    $('#id').val(data.data.id);
                    $('#name2').val(data.data.name);
                    $('#age2').val(data.data.age);
                } else {
                    alert(data.msg);
                }
            },
            error: function () {
                alert('网络错误，请稍后重试！');
            }
        });
    }

    // 删除按钮点击事件
    function del(id) {
        if (confirm('确定要删除吗？')) {
            $.ajax({
                url: '/api/delete',
                type: 'DELETE',
                data: {
                    id: id
                },
                success: function (data) {
                    if (data.code === 0) {
                        $('#searchBtn').click();
                    } else {
                        alert(data.msg);
                    }
                },
                error: function () {
                    alert('网络错误，请稍后重试！');
                }
            });
        }
    }

    function updatePagination() {
        var pagination = $('.pagination');
        pagination.empty();
        if (totalPage <= 1) {
            return;
        }
        var html = '';
        if (currentPage > 1) {
            html += '<li><a href="#" onclick="gotoPage(' + (currentPage - 1) + ')" aria-label="Previous"><span aria-hidden="true">&laquo;</span></a></li>';
        } else {
            html += '<li class="disabled"><a href="#" aria-label="Previous"><span aria-hidden="true">&laquo;</span></a></li>';
        }
        var startPage = Math.max(1, currentPage - 2);
        var endPage = Math.min(totalPage, currentPage + 2);
        for (var i = startPage; i <= endPage; i++) {
            if (i == currentPage) {
                html += '<li class="active"><a href="#">' + i + '</a></li>';
            } else {
                html += '<li><a href="#" onclick="gotoPage(' + i + ')">' + i + '</a></li>';
            }
        }
        if (currentPage < totalPage) {
            html += '<li><a href="#" onclick="gotoPage(' + (currentPage + 1) + ')" aria-label="Next"><span aria-hidden="true">&raquo;</span></a></li>';
        } else {
            html += '<li class="disabled"><a href="#" aria-label="Next"><span aria-hidden="true">&raquo;</span></a></li>';
        }
        pagination.html(html);
    }

    function gotoPage(page) {
        currentPage = page;
        $('#searchBtn').click();
    }
</script>



</html>

```
